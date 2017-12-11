package com.hzcard.syndata.datadeal

import java.util.concurrent.ArrayBlockingQueue

import akka.actor.Status.{Failure, Status, Success}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, ActorRefFactory, ActorSystem, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.{DealDataStatus, SpringExtentionImpl}
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import org.springframework.stereotype.Component

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}

/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("dbDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DbDealDataActor(@Autowired clientProperties: CanalClientProperties,
                      @Autowired mapDataSourceLookup: MapDataSourceLookup,
                      @Autowired @Qualifier("esDealDataActorRef") getNextActor: ActorRef) extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  import context.dispatcher

  implicit val timeout = Timeout(60 seconds)

  val tableNameCache = new ArrayBlockingQueue[String](1)

  val tableDealDataSize = new mutable.HashMap[String, Long]()

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  override def preStart(): Unit = {
    log.info("DbDealDataActor is start")
    super.preStart()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    if (reason.getMessage == null || reason.getMessage.indexOf("Deadlock") < 0)
      log.error(s"DbDealDataActor preRestart ,reason is ${reason.getMessage} ,message is ${message}", reason)
    if (!message.isEmpty && message.get.isInstanceOf[DealDataRequestModel]) { //遇到了异常，重启的时候，重复处理数据，需要注意死循环
      dealDataRequestModel(message.get.asInstanceOf[DealDataRequestModel])
    }
  }

  override def receive: Receive = {
    case x: DealDataRequestModel => dealDataRequestModel(x)
    case x: BinLogPosition => {
      if (log.isDebugEnabled)
        log.debug(s"receive binlogPosiont ${x}")
      getNextActor ! x
    }
    case x =>
      throw new RuntimeException(s"Received invalid message.,message is ${x}")
  }

  def dealDataRequestModel(x: DealDataRequestModel) {
    val startTime = System.currentTimeMillis()
    val metaData = getMetaData(x.data._1._1, x.data._1._2, x.data._2(0))
    if (metaData._4) {
      val data = DealDataRequest(mapDataSourceLookup, metaData._1, metaData._2, metaData._3, x.data._2, x.txId)
      dbDeal(data)
      val time = System.currentTimeMillis() - startTime
      if (log.isWarnEnabled && time > 1000)
        log.warn(s"操作数据库 ${x.data._1._1}.${x.data._1._2} 耗时 ${time}")
    }

    val esDeal = getNextActor ? x
    Await.result(esDeal, timeout.duration).asInstanceOf[DealDataStatus]
    if (log.isDebugEnabled)
      log.debug(s"finish dealData  binlog txId is ${x.txId}")
    sender() ! DealDataStatus.Success

  }

  def dbDeal(x: DealDataRequest): Unit = try {
    val newArray = merger(x.dataArray)
    for (data <- newArray) {
      val nowSchmaTab = s"${data.get.schema}.${data.get.tableName}"
      var isSuccess = false
      val targetTableName = mapTargetTableName(x.schema,x.tableName)
      if (data.get.eventType == EventType.INSERT) {
        while (!isSuccess) {
          try {
            log.warn(s"deal data ${x.schema}.${targetTableName},txId is ${x.txId},data size is ${data.get.rowChanges.size}")
            isSuccess = DBOperationScala(x.mapDataSource).save(x.schema, targetTableName, x.keyword, data.get.rowChanges.toArray, x.txId,data.get.columnTypes)
          } catch {
            case th: Throwable =>
              log.error(s" insert DB data ${x.schema}.${targetTableName},exception is ${th.getMessage}", th)
          }
        }

      } else if (data.get.eventType == EventType.UPDATE) {
        while (!isSuccess) {
          try {
            isSuccess = DBOperationScala(x.mapDataSource).update(x.schema, targetTableName, x.keyword, data.get.rowChanges.toArray,data.get.columnTypes)
          } catch {
            case th: Throwable =>
              log.error(s" update DB data ${x.schema}.${targetTableName},exception is ${th.getMessage}", th)
          }
        }
      } else {
        while (!isSuccess) {
          try {
            isSuccess = DBOperationScala(x.mapDataSource).delete(x.schema, targetTableName, x.keyword, data.get.rowChanges.toArray)
          } catch {
            case th: Throwable =>
              log.error(s" dele DB data ${x.schema}.${targetTableName},exception is ${th.getMessage}", th)
          }
        }
      }
    }
  } catch {
    case el: Throwable => {
      log.error(s"dbDealDataActor exception ${el.getMessage}", el)
      throw el
    }
  }

  def mapTargetTableName(schema:String, mysqlTableName:String): String ={
    if(clientProperties.getSchemas.get(schema).getTableNameMappings==null)
      mysqlTableName
    else if(clientProperties.getSchemas.get(schema).getTableNameMappings.containsKey(mysqlTableName))
      clientProperties.getSchemas.get(schema).getTableNameMappings.get(mysqlTableName)
    else
      mysqlTableName
  }

  def getMetaData(schema: String, tableName: String, schemaTableMapData: SchemaTableMapData) = {
    var keyWord = "id"
    var isSynTable = false

    val includeSynTables: Array[String] = {
      if (clientProperties.getSchemas.get(schema) != null && clientProperties.getSchemas.get(schema).getIncludeSynTables != null)
        clientProperties.getSchemas.get(schema).getIncludeSynTables.split(",")
      else null
    }
    if (includeSynTables == null || includeSynTables.length == 0)
      isSynTable = false
    else
      for (includeSynTable <- includeSynTables) {
        if (includeSynTable.equals("*"))
          isSynTable = true
        if (!isSynTable && includeSynTable.equalsIgnoreCase(tableName))
          isSynTable = true
      }
    if (isSynTable) {
      val excludeSynTables: Array[String] = {
        if (clientProperties.getSchemas.get(schema) != null && clientProperties.getSchemas.get(schema).getExcludeSynTables != null)
          clientProperties.getSchemas.get(schema).getExcludeSynTables.split(",")
        else
          null
      }
      if (excludeSynTables != null) {
        for (excludeSynTable <- excludeSynTables) {
          if (isSynTable && excludeSynTable.equalsIgnoreCase(tableName))
            isSynTable = false
        }
      }
    }
    keyWord = schemaTableMapData.primaryKeys.keySet.mkString(",") //获得主键，关键字
    (schema, tableName, keyWord, isSynTable)
  }
}

case class DealDataRequest(override val mapDataSource: MapDataSourceLookup, schema: String, tableName: String, keyword: String, dataArray: Array[SchemaTableMapData], txId: String) extends ParentRequest(mapDataSource)

case class TransComplete(txId: String, isEsDeal: Boolean)

case class RetryNeed(txId: String)