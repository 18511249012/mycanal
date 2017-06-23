package com.hzcard.syndata.extractlog

import akka.actor.{ActorRef, ActorSystem, Props}
import com.github.shyiko.mysql.binlog.BinaryLogClient.EventListener
import com.github.shyiko.mysql.binlog.event.EventType._
import com.github.shyiko.mysql.binlog.event._
import com.hzcard.syndata.config.autoconfig.{DestinationProperty, Encryptor}
import com.hzcard.syndata.extractlog.actors.{ColumnInfoActor, JsonFormatterActor, TransactionActor}
import com.hzcard.syndata.extractlog.emitter.EmitterActor
import com.hzcard.syndata.extractlog.events._
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext


object ChangeStreamEventListener {
  def apply(system: ActorSystem, applicationContext: ApplicationContext): ChangeStreamEventListener = new ChangeStreamEventListener(system, applicationContext)
}

class ChangeStreamEventListener(system: ActorSystem, val applicationContext: ApplicationContext) extends EventListener {
  protected val log = LoggerFactory.getLogger(getClass)
  protected val systemDatabases = Seq("information_schema", "mysql", "performance_schema", "sys")
  protected val whitelist: java.util.List[String] = new java.util.LinkedList[String]()
  protected val blacklist: java.util.List[String] = new java.util.LinkedList[String]()


  private var emitterLoader: Option[ActorRef] = None
  private var formatterActor: Option[ActorRef] = None
  private var columnInfoActor: Option[ActorRef] = None
  private var transactionActor: Option[ActorRef] = None

  /** Allows the configuration for the listener object to be set on startup.
    * The listener will look for whitelist, blacklist, and emitter settings.
    *
    * @param config
    */
  def setConfig(config: DestinationProperty, encryptor: Encryptor) = {
    whitelist.clear()
    blacklist.clear()
    if (StringUtils.isNotEmpty(config.getMysql.getWhitelist)) {
      config.getMysql.getWhitelist.split(',').foreach(whitelist.add(_))
      log.info(s"Using event whitelist: ${whitelist}")
    }
    else if (StringUtils.isNotEmpty(config.getMysql.getBlacklist)) {
      config.getMysql.getBlacklist.split(',').foreach(blacklist.add(_))
      log.info(s"Using event blacklist: ${blacklist}")
    }
    emitterLoader = Some(system.actorOf(Props(new EmitterActor(config.getMysql, applicationContext)), name = "emitterActor" + config.getMysql.getMyChannel))
    formatterActor = Some(system.actorOf(Props(new JsonFormatterActor(_ => emitterLoader.get, config.getMysql, encryptor)), name = "formatterActor" + config.getMysql.getMyChannel))
    columnInfoActor = Some(system.actorOf(Props(new ColumnInfoActor(_ => formatterActor.get, config.getMysql)), name = "columnInfoActor" + config.getMysql.getMyChannel))
    transactionActor = Some(system.actorOf(Props(new TransactionActor(_ => columnInfoActor.get)), name = "transactionActor" + config.getMysql.getMyChannel))
    log.info(s"init ${config.getMysql.getMyChannel} actor emitterLoader=${emitterLoader},formatterActor=${formatterActor},columnInfoActor = ${columnInfoActor},transactionActor = ${transactionActor}")
    this
  }

  /** Sends binlog events to the appropriate changestream actor.
    *
    * @param binaryLogEvent The binlog event
    */
  def onEvent(binaryLogEvent: Event) = {
    if (log.isDebugEnabled)
      log.debug(s"Received event: ${binaryLogEvent.getHeader[EventHeaderV4].getEventType}")
    val changeEvent = getChangeEvent(binaryLogEvent)

    changeEvent match {
      case Some(e: TransactionEvent) => transactionActor.get ! e
      case Some(e: MutationEvent) => transactionActor.get ! MutationWithInfo(e)
      case Some(e: AlterTableEvent) => columnInfoActor.get ! e
      case Some(e: RotateEvent) => transactionActor.get ! e
      case None =>
        log.debug(s"Ignoring ${binaryLogEvent.getHeader[EventHeaderV4].getEventType} event.")
    }
  }

  /** Returns the appropriate ChangeEvent case object given a binlog event object.
    *
    * @param event The java binlog listener event
    * @return The resulting ChangeEvent
    */
  def getChangeEvent(event: Event): Option[ChangeEvent] = {
    val header = event.getHeader[EventHeaderV4]
//    log.warn(s"event is ${header.getEventType}")
    header.getEventType match {
      case eventType if EventType.isRowMutation(eventType) =>
        getMutationEvent(event, header)

      case GTID =>
        Some(Gtid(event.getData[GtidEventData].getGtid))

      case XID =>
        Some(CommitTransaction(header.getNextPosition))

      case QUERY =>
        parseQueryEvent(event.getData[QueryEventData], header)

      case FORMAT_DESCRIPTION =>
        val data = event.getData[FormatDescriptionEventData]
        log.debug(s"Server version: ${data.getServerVersion}, binlog version: ${data.getBinlogVersion}")
        None

      // Known events that are safe to ignore
      case PREVIOUS_GTIDS => None
      case ROTATE => Some(RotateEvent(event.getData[RotateEventData].getBinlogFilename, event.getData[RotateEventData].getBinlogPosition))
      case ROWS_QUERY => None
      case TABLE_MAP => None
      case ANONYMOUS_GTID => None
      case STOP => None

      case _ =>
        val message = s"Received unknown message: ${event}"
        log.error(message)
        throw new Exception(message)
    }
  }

  protected def getMutationEvent(event: Event, header: EventHeaderV4): Option[MutationEvent] = {
    val mutation = header.getEventType match {
      case e if EventType.isWrite(e) =>
        event.getData[Insert].copy(timestamp = header.getTimestamp, binLogPositon = header.getNextPosition)

      case e if EventType.isUpdate(e) =>
        event.getData[Update].copy(timestamp = header.getTimestamp, binLogPositon = header.getNextPosition)

      case e if EventType.isDelete(e) =>
        event.getData[Delete].copy(timestamp = header.getTimestamp, binLogPositon = header.getNextPosition)
    }

    shouldIgnore(mutation) match {
      case true =>
        log.debug(s"Ignoring event for table ${mutation.database}.${mutation.tableName}.")
        None
      case false =>
        Some(mutation)
    }
  }

  /** Returns a ChangeEvent case class instance representing the change indicated by
    * the given binlog QUERY event (either BEGIN, COMMIT, ROLLBACK, or ALTER...).
    *
    * @param queryData The QUERY event data
    * @return
    */
  protected def parseQueryEvent(queryData: QueryEventData, header: EventHeaderV4): Option[ChangeEvent] = {
    queryData.getSql match {
      case sql if sql matches "(?i)^begin" =>
        Some(BeginTransaction)

      case sql if sql matches "(?i)^commit" =>
        Some(CommitTransaction(header.getNextPosition))

      case sql if sql matches "(?i)^rollback" =>
        Some(RollbackTransaction(header.getNextPosition))

      case sql if sql matches "(?i)^alter.*" =>

        /** Not(space, dot)+ OR backtick + Not(backtick, dot) + backtick OR "Not(", dot)" **/
        /** Does not currently support spaces or backticks in table or db names **/
        val dbOrTableName = "([^\\s\\.]+|`[^`\\.]+`|\"[^\"\\.]\")"
        val identifierRegex = s"(?i)^alter\\s+(ignore\\s+)?table\\s+($dbOrTableName(\\.$dbOrTableName)?).*".r

        var maybeIdentifier: Option[(String, String)] = None
        for {p <- identifierRegex findAllIn sql} p match {
          case identifierRegex(_, _, first, _, second) =>
            maybeIdentifier = Some(second match {
              case null => (queryData.getDatabase, unescapeIdentifier(first)) //scalastyle:ignore
              case _ => (unescapeIdentifier(first), unescapeIdentifier(second))
            })
        }

        maybeIdentifier.map({
          case (db, table) => Some(AlterTableEvent(db.toLowerCase, table.toLowerCase, sql))
        }).getOrElse(
          None
        )
      case sql =>
        None
    }
  }

  protected def unescapeIdentifier(escaped: String) = escaped.charAt(0) match {
    case '`' => escaped.substring(1, escaped.length - 1).replace("``", "`")
    case '"' => escaped.substring(1, escaped.length - 1).replace("\"\"", "\"")
    case _ => escaped
  }

  private def shouldIgnore(info: MutationEvent) = {
    if (systemDatabases.contains(info.database)) {
      true
    }
    else if (!whitelist.isEmpty) {
      tableMissingFromWhitelist(info.database, info.tableName)
    }
    else if (!blacklist.isEmpty) {
      tableInBlacklist(info.database, info.tableName)
    }
    else {
      false
    }
  }

  private def tableInBlacklist(database: String, table: String) = {
    tableInList(database, table, blacklist)
  }

  private def tableMissingFromWhitelist(database: String, table: String) = {
    !tableInList(database, table, whitelist)
  }

  private def tableInList(database: String, table: String, list: java.util.List[String]) = {
    if (list.isEmpty) {
      false
    }
    else if (list.contains(s"${database}.*") || list.contains(s"${database}.${table}")) {
      true
    }
    else {
      false
    }
  }
}
