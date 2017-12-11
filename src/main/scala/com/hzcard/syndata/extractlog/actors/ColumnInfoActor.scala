package com.hzcard.syndata.extractlog.actors

import java.sql.ResultSet

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.SpringExtentionImpl
import com.hzcard.syndata.datadeal.DealDataRequestModel
import com.hzcard.syndata.extractlog.events._
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import org.springframework.stereotype.Component

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps


case class PendingMutation(schemaSequence: Long, event: MutationWithInfo)

@Component("columnInfoActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ColumnInfoActor(
                       @Autowired dataSourceLookup: MapDataSourceLookup, @Autowired @Qualifier("jsonFormatterActorRef") nextHop: ActorRef
                     ) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  val COLUMN_NAME = 1
  val DATA_TYPE = 2
  val IS_PRIMARY = 3
  val DATABASE_NAME = 4
  val TABLE_NAME = 5

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  protected val _schemaSequence = mutable.HashMap.empty[String, Long]

  protected def getNextSchemaSequence(myChannel: String): Long = {
    if (_schemaSequence.get(myChannel).isEmpty)
      _schemaSequence.put(myChannel, -1L)

    _schemaSequence.put(myChannel, _schemaSequence.get(myChannel).get + 1)
    _schemaSequence.get(myChannel).get
  }

  protected val columnsInfoCache = mutable.HashMap.empty[String, mutable.HashMap[(String, String), ColumnsInfo]]
//  protected val mutationBuffer = mutable.HashMap.empty[String, mutable.HashMap[(String, String), List[PendingMutation]]]


  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    reason match {
      case ColumInfoNotMatchException(message:String) =>
      case oth if(!message.isEmpty) => {
        log.warn(s"retrying send message ${oth}")
        self ! oth
      }
    }
  }

  def receive = {
    case event: MutationWithInfo => try {
      if (log.isDebugEnabled)
        log.debug(s"Received mutation event on table ${event.mutation.cacheKey},txId is ${event.transaction.get.gtid}，lastmu is ${event.transaction.get.lastMutationInTransaction}")
      if (columnsInfoCache.get(event.myChannel.get).isEmpty) {
        columnsInfoCache.put(event.myChannel.get, mutable.HashMap.empty[(String, String), ColumnsInfo])
      }

      columnsInfoCache.get(event.myChannel.get).get.get(event.mutation.cacheKey) match {
        case info: Some[ColumnsInfo] =>
          nextHop ! event.copy(columns = info)
        case None =>
          val pending = PendingMutation(getNextSchemaSequence(event.myChannel.get), event)
          requestColumnInfo(event.myChannel.get, pending.schemaSequence, event.mutation.database, event.mutation.tableName)
          nextHop ! event.copy(columns = Some(columnsInfoCache.get(event.myChannel.get).get(event.mutation.cacheKey)))
      }
    } catch {
      case x: Throwable =>
        log.error("ColumnInfoActor exception " + x.getMessage, x)
        throw x
      //        sender() ! akka.actor.Status.Failure(x)
    }

    case alter: AlterTableEvent =>
      if (log.isDebugEnabled)
        log.debug(s"Refreshing the cache due to alter table (${alter.cacheKey}): ${alter.sql}")
      if (!columnsInfoCache.get(alter.myChannel).isEmpty)
        columnsInfoCache.get(alter.myChannel).get.remove(alter.cacheKey)
      requestColumnInfo(alter.myChannel, getNextSchemaSequence(alter.myChannel), alter.database, alter.tableName)

    case othe =>
      log.error("Invalid message received by ColumnInfoActor,message is ${othe}")
      throw new ColumInfoNotMatchException(s"Invalid message received by ColumnInfoActor,message is ${othe}")
  }

  protected def requestColumnInfo(myChannel: String, schemaSequence: Long, database: String, tableName: String) = {
    val columnInfo = getColumnsInfo(myChannel, schemaSequence, database, tableName)
    columnInfo match {
      case Some(result) => {
        if (columnsInfoCache.get(myChannel).isEmpty)
          columnsInfoCache.put(myChannel, mutable.HashMap.empty[(String, String), ColumnsInfo])
        columnsInfoCache.get(myChannel).get.put(result.cacheKey, result)
      }
      case None => throw new RuntimeException(s"No column metadata found for table ${database}.${tableName}")
    }
  }

  protected def getColumnsInfo(myChannel: String, schemaSequence: Long, database: String, tableName: String): Option[ColumnsInfo] = {

    val escapedDatabase = database.replace("'", "\\'")
    val escapedTableName = tableName.replace("'", "\\'")
    val dataSouce = dataSourceLookup.getDataSource("channel" + myChannel)
    val conn = dataSouce.getConnection
    try {
      val sql = "select col.COLUMN_NAME," +
        "col.DATA_TYPE,case when pk.COLUMN_NAME is not null then true  else false end as IS_PRIMARY " +
        "from INFORMATION_SCHEMA.COLUMNS col " +
        "left outer join INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk on col.TABLE_SCHEMA = pk.TABLE_SCHEMA and pk.CONSTRAINT_NAME = 'PRIMARY' " +
        "and col.TABLE_NAME = pk.TABLE_NAME and col.COLUMN_NAME = pk.COLUMN_NAME " +
        "where col.TABLE_SCHEMA = ? and col.TABLE_NAME = ? order by col.ORDINAL_POSITION"
      val preparedStatement = conn.prepareStatement(sql)
      preparedStatement.setString(1, escapedDatabase)
      preparedStatement.setString(2, escapedTableName)
      val result = preparedStatement.executeQuery()
      val rowData = new ArrayBuffer[Column]
      while (result.next()) {
        rowData += getColumnForRow(result)
      }
      result.close()
      preparedStatement.close()
      conn.close()
      if(rowData.size==0)
        throw new RuntimeException("columinfo not get")
      Some(ColumnsInfo(
        schemaSequence,
        database,
        tableName,
        rowData
      ))

    } catch {
      case x: Throwable =>
        log.error(s"query mysql matainfo exception myChannel is ${myChannel} , database is ${database} , tableName is ${tableName}", x)
        throw x
    } finally
      if (!conn.isClosed)
        conn.close()

  }


  protected def getColumnForRow(row: ResultSet): Column = {
    Column(
      name = row.getString(COLUMN_NAME),
      dataType = row.getString(DATA_TYPE),
      isPrimary = row.getBoolean(IS_PRIMARY)
    )
  }


}

case class  ColumInfoNotMatchException(message:String) extends Throwable(message)
