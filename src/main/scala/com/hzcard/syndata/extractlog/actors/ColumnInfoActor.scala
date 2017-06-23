package com.hzcard.syndata.extractlog.actors

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, ActorRefFactory, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.github.mauricio.async.db.util.ExecutorServiceUtils
import com.github.mauricio.async.db.{Configuration, RowData}
import com.hzcard.syndata.config.autoconfig.MysqlClientProperties
import com.hzcard.syndata.extractlog.events._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ColumnInfoActor {
  val COLUMN_NAME = 0
  val DATA_TYPE = 1
  val IS_PRIMARY = 2
  val DATABASE_NAME = 3
  val TABLE_NAME = 4
  val PRELOAD_POSITION = 0

  case class PendingMutation(schemaSequence: Long, event: MutationWithInfo)

}
class ColumnInfoActor(
                       getNextHop: ActorRefFactory => ActorRef,
                       config: MysqlClientProperties
                     ) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  import ColumnInfoActor._
  import context.dispatcher

  override val supervisorStrategy =
    OneForOneStrategy(loggingEnabled = true) {
      case _: MySQLException => Restart
      case _: Exception => Escalate
    }

  protected val log = LoggerFactory.getLogger(getClass)
  protected val nextHop = getNextHop(context)

  protected val PRELOAD_TIMEOUT = config.getPreloadTimeOut.longValue()
  protected val preloadDatabases = config.getPreloadDatabases

  protected val TIMEOUT = config.getTimeout.longValue()
  protected val mysqlConfig = new Configuration(
    config.getUser,
    config.getHost,
    config.getPort,
    Some(config.getPassword)
  )
  private val pool = new ConnectionPool(new MySQLConnectionFactory(mysqlConfig), PoolConfiguration.Default, ExecutionContext.fromExecutor(ExecutorServiceUtils.newFixedPool(10, "db-async-default")))
  protected var _schemaSequence = -1
  protected def getNextSchemaSequence: Long = {
    _schemaSequence += 1
    _schemaSequence
  }
  protected val columnsInfoCache = mutable.HashMap.empty[(String, String), ColumnsInfo]
  protected val mutationBuffer = mutable.HashMap.empty[(String, String), List[PendingMutation]]

  override def preStart() = {
    val connectRequest = pool.connect

    connectRequest onComplete {
      case Success(result) =>
        log.info("Connected to MySQL server for Metadata!!")
      case Failure(exception) =>
        log.error("Could not connect to MySQL server for Metadata", exception)
        throw exception
    }
    Await.result(connectRequest, TIMEOUT milliseconds)
    preLoadColumnData
  }

  override def postStop() = {
    Await.result(pool.disconnect, TIMEOUT milliseconds)
  }

  def receive = {
    case event: MutationWithInfo => try {
      if (log.isInfoEnabled)
        log.info(s"Received mutation event on table ${event.mutation.cacheKey},txId is ${event.transaction.get.gtid}")
      columnsInfoCache.get(event.mutation.cacheKey) match {
        case info: Some[ColumnsInfo] =>
          nextHop ! event.copy(columns = info)
        case None =>
          if (log.isInfoEnabled)
            log.info(s"Couldn't find column info for event on table ${event.mutation.cacheKey} -- buffering mutation and kicking off a query")
          val pending = PendingMutation(getNextSchemaSequence, event)
          mutationBuffer(event.mutation.cacheKey) = mutationBuffer.get(event.mutation.cacheKey).fold(List(pending))(buffer =>
            buffer :+ pending
          )
          requestColumnInfo(pending.schemaSequence, event.mutation.database, event.mutation.tableName)
      }
      mutationBuffer.remove(event.mutation.cacheKey).foreach({ bufferedMutations =>
        val (ready, stillPending) = bufferedMutations.partition(mutation => columnsInfoCache.get(event.mutation.cacheKey).get.schemaSequence <= mutation.schemaSequence)
        mutationBuffer.put(columnsInfoCache.get(event.mutation.cacheKey).get.cacheKey, stillPending)
        if (ready.size > 0) {
          if (log.isDebugEnabled)
            log.debug(s"Adding column info and forwarding ${ready.size} mutations to the ${nextHop.path.name} actor")
          ready.foreach(nextHop ! _.event.copy(columns = Some(columnsInfoCache.get(event.mutation.cacheKey).get)))
        }
      })
//      TimeUnit.MILLISECONDS.sleep(10L)
    } catch {
      case x: Throwable =>
        log.error("ColumnInfoActor exception " + x.getMessage, x)
        sender() ! akka.actor.Status.Failure(x)
    }

    case alter: AlterTableEvent =>
      if (log.isDebugEnabled)
        log.debug(s"Refreshing the cache due to alter table (${alter.cacheKey}): ${alter.sql}")
      columnsInfoCache.remove(alter.cacheKey)
      requestColumnInfo(getNextSchemaSequence, alter.database, alter.tableName)

    case _ =>
      log.error("Invalid message received by ColumnInfoActor")
      throw new Exception("Invalid message received by ColumnInfoActor")
  }

  protected def requestColumnInfo(schemaSequence: Long, database: String, tableName: String) = {
    val future = getColumnsInfo(schemaSequence, database, tableName)
    if (log.isDebugEnabled)
      log.debug(s"wait future column inf return ${database},${tableName}")
    val columnInfo = Await.result(future, Duration.Inf).asInstanceOf[Option[ColumnsInfo]]
    if (log.isDebugEnabled)
      log.debug(s"wait future column return complete ${database},${tableName}")
    columnInfo match {
      case Some(result) => columnsInfoCache(result.cacheKey) = result
      case None => throw new RuntimeException(s"No column metadata found for table ${database}.${tableName}")
    }
  }

  protected def getColumnsInfo(schemaSequence: Long, database: String, tableName: String): Future[Option[ColumnsInfo]] = {

    val escapedDatabase = database.replace("'", "\\'")
    val escapedTableName = tableName.replace("'", "\\'")
    pool
      .sendQuery(
        s"""
           | select
           |   col.COLUMN_NAME,
           |   col.DATA_TYPE,
           |   case when pk.COLUMN_NAME is not null then true else false end as IS_PRIMARY
           | from INFORMATION_SCHEMA.COLUMNS col
           | left outer join INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk
           |   on col.TABLE_SCHEMA = pk.TABLE_SCHEMA
           |   and col.TABLE_NAME = pk.TABLE_NAME
           |   and col.COLUMN_NAME = pk.COLUMN_NAME
           | where col.TABLE_SCHEMA = '${escapedDatabase}'
           |   and col.TABLE_NAME = '${escapedTableName}' order by col.ORDINAL_POSITION
      """.stripMargin)
      .map(_.rows.map(_.map(getColumnForRow)) match {
        case Some(list) if !list.isEmpty =>
          Some(ColumnsInfo(
            schemaSequence,
            database,
            tableName,
            list
          ))
        case _ =>
          None
      })
  }

  protected def preLoadColumnData = {
    if (!preloadDatabases.isEmpty) {
      val request = for {
        columnsInfo <- getAllColumnsInfo recover {
          case exception =>
            log.error(s"Couldn't fetch metadata for databases: ${preloadDatabases}", exception)
            throw exception
        }
      } yield columnsInfo.foreach {
        case table: ColumnsInfo =>
          columnsInfoCache(table.cacheKey) = table
      }

      Await.result(request, PRELOAD_TIMEOUT milliseconds)
    }
  }

  protected def getAllColumnsInfo: Future[Iterable[ColumnsInfo]] = {
    val databases = preloadDatabases.replace("'", "\'").split(",").mkString("','")

    pool
      .sendQuery(
        s"""
           | select
           |   lower(col.COLUMN_NAME),
           |   lower(col.DATA_TYPE),
           |   case when pk.COLUMN_NAME is not null then true else false end as IS_PRIMARY,
           |   lower(col.TABLE_SCHEMA) as DATABASE_NAME,
           |   lower(col.TABLE_NAME)
           | from INFORMATION_SCHEMA.COLUMNS col
           | left outer join INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk
           |   on col.TABLE_SCHEMA = pk.TABLE_SCHEMA
           |   and col.TABLE_NAME = pk.TABLE_NAME
           |   and col.COLUMN_NAME = pk.COLUMN_NAME
           | where col.TABLE_SCHEMA in ('${databases}') order by col.ORDINAL_POSITION
      """.stripMargin)
      .map(queryResult =>
        queryResult.rows.map(
          _.groupBy(row =>
            (
              row(DATABASE_NAME).asInstanceOf[String],
              row(TABLE_NAME).asInstanceOf[String]
            )) collect {
            case ((database, table), rows) =>
              ColumnsInfo(
                getNextSchemaSequence,
                database,
                table,
                rows.map(getColumnForRow)
              )
          }
        ).getOrElse(Iterable.empty))
  }

  protected def getColumnForRow(row: RowData): Column = {
    Column(
      name = row(COLUMN_NAME).asInstanceOf[String],
      dataType = row(DATA_TYPE).asInstanceOf[String],
      isPrimary = row(IS_PRIMARY) match {
        case 0 => false
        case 1 => true
      }
    )
  }
}
