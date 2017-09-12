package com.hzcard.syndata.datadeal

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup

import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/3/6.
  */

class DbDealDataActor(getNextHop: ActorRefFactory => ActorRef,
                      applicationContext: ApplicationContext) extends Actor with MergeOperationArray with ActorLogging with RequiresMessageQueue[BoundedMessageQueueSemantics]{

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  val getNextActor = getNextHop(context)
  val clientProperties = applicationContext.getBean(classOf[CanalClientProperties])
  val mapDataSourceLookup = applicationContext.getBean(classOf[MapDataSourceLookup])


  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage,reason)
    if(!message.isEmpty && message.get.isInstanceOf[DealDataRequestModel]){   //遇到了异常，重启的时候，重复处理数据，需要注意死循环
      dealDataRequestModel(message.get.asInstanceOf[DealDataRequestModel])
    }
  }

  override def receive: Receive = {
    case x: DealDataRequestModel => dealDataRequestModel(x)
    case x: BinLogPosition => getNextActor ! x
    case x =>
      throw new RuntimeException(s"Received invalid message.")
  }


//  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
//    case _: Exception                => Restart
//  }

  def dealDataRequestModel(x: DealDataRequestModel)  {
    val startTime = System.currentTimeMillis()
    val metaData = getMetaData(x.data._1._1, x.data._1._2)
    if (metaData._4) {
      val data = DealDataRequest(mapDataSourceLookup, metaData._1, metaData._2, metaData._3, x.data._2, x.txId)
      dbDeal(data)
    }
    if(log.isInfoEnabled)
      log.info(s"操作数据库 ${x.data._1._1}.${x.data._1._2} 耗时 ${System.currentTimeMillis() - startTime}")
    getNextActor ! x
  }

  def dbDeal(x: DealDataRequest): Unit = try {
    val newArray = merger(x.dataArray)
    for (data <- newArray) {
      if (data.get.eventType == EventType.INSERT) {
        DBOperationScala(x.mapDataSource).save(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)

      } else if (data.get.eventType == EventType.UPDATE) {
        DBOperationScala(x.mapDataSource).update(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)


      } else {
        DBOperationScala(x.mapDataSource).delete(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)
      }
    }
  } catch {
    case el: Throwable => {
      log.error(s"dbDealDataActor exception ${el.getMessage}", el)
      throw el
    }
  }
  def getMetaData(schema: String, tableName: String) = {
    var keyWord = "id"
    var isSynTable = false
    val includeSynTablesStr = {
      if (clientProperties.getSchemas.get(schema) != null)
        clientProperties.getSchemas.get(schema).getIncludeSynTables
      else null
    }
    val includeSynTables: Array[String] = {
      if (includeSynTablesStr != null)
        includeSynTablesStr.split(",")
      else
        null
    }
    if (includeSynTables == null || includeSynTables.length == 0)
      isSynTable = false
    else
      for (includeSynTable <- includeSynTables) {
        if (includeSynTable.equalsIgnoreCase(tableName))
          isSynTable = true
      }
    if (clientProperties.getSchemas.get(schema) != null
      && clientProperties.getSchemas.get(schema).getTableRepository != null
      && clientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      keyWord = if (clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword == null) "id" else clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword
    (schema, tableName, keyWord, isSynTable)
  }
}

case class DealDataRequest(override val mapDataSource: MapDataSourceLookup, schema: String, tableName: String, keyword: String, dataArray: Array[SchemaTableMapData], txId: String) extends ParentRequest(mapDataSource)

case class TransComplete(txId: String, isEsDeal: Boolean)

case class RetryNeed(txId: String)