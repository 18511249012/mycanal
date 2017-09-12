package com.hzcard.syndata.datadeal

import java.util

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import org.springframework.context.ApplicationContext
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.data.util.ClassTypeInformation

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/3/6.
  */

class EsDealDataActor(getNextHop: ActorRefFactory => ActorRef,
                      applicationContext: ApplicationContext) extends Actor with MergeOperationArray with ActorLogging with RequiresMessageQueue[BoundedMessageQueueSemantics]{


  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  val clientProperties = applicationContext.getBean(classOf[CanalClientProperties])
  val getNextActor = getNextHop(context)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage,reason)
    if (!message.isEmpty && message.get.isInstanceOf[DealDataRequestModel]) { //遇到了异常，重启的时候，重复处理数据，需要注意死循环
      dealDataRequestModel(message.get.asInstanceOf[DealDataRequestModel])
    }
  }
//  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
//    case _: Exception                => Restart
//  }

  override def receive: Receive = {
    case x: DealDataRequestModel => dealDataRequestModel(x)
    case x: BinLogPosition => getNextActor ! x
    case _ =>
      throw new RuntimeException(s"Received invalid message.")
  }

  def dealDataRequestModel(x: DealDataRequestModel) = try {
    val startTime = System.currentTimeMillis()
    val metaData = getMetaData(x.data._1._1, x.data._1._2)
    if (metaData._3) {
      val esDealDataRequest = EsDealDataRequest(metaData._4, mapToEntity(x.data._2, metaData._5), x.txId)
      dealEs(esDealDataRequest)
    }
    if(log.isInfoEnabled)
      log.info(s"操作es耗时 ${x.data._1._1}.${x.data._1._2} 耗时 ${System.currentTimeMillis() - startTime}")
//    getNextActor ! x
  } catch {
    case th: Throwable =>
      log.error(th.getMessage, th)
      throw th
  }

  def dealEs(x: EsDealDataRequest) = //处理下针对es的数组，将update操作当成insert操作合并
    try {
      val esChangeArray = x.dataArray.map(x => if (x.eventType == EventType.UPDATE) SchemaTableMapData(x.schema, x.tableName, EventType.INSERT, x.rowChange) else x)
      val newArray = merger(esChangeArray)
      newArray.foreach(data => {
        val entitys = mapToEntity(data.get.rowChanges, x.repository).asInstanceOf[java.lang.Iterable[Object]]
        if (data.get.eventType == EventType.DELETE)
          x.repository.delete(entitys)
        else
          x.repository.save(entitys)
      })
    } catch {
      case e: Throwable =>
        log.error(s"EsDealDataActor exception ${e.getMessage}", e)
        throw e
    }

  def getMetaData(schema: String, tableName: String) = {
    var repository: String = null
    if (clientProperties.getSchemas.get(schema) != null &&
      clientProperties.getSchemas.get(schema).getTableRepository != null
      && clientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      repository = clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getRepository
    var repositoryBean: ElasticsearchRepository[Object, String] = null
    var isSynEss = true
    if (repository == null || repository.trim.length == 0)
      isSynEss = false
    else
      repositoryBean = applicationContext.getBean(Class.forName(repository)).asInstanceOf[ElasticsearchRepository[Object, String]]
    (schema, tableName, isSynEss, repositoryBean, repository)
  }

  @throws[Exception]
  def mapToEntity(maps: Array[SchemaTableMapData], repository: String): Array[SchemaTableMapData] = {
    if ("com.hzcard.syndata.points.repositories.OperationOrderRepository" == repository || "com.hzcard.syndata.points.repositories.OrderRepository" == repository) {
      for (sigle <- maps) {
        sigle.rowChange.put("code_search", sigle.rowChange.get("code").get)
        sigle.rowChange.put("card_no_search", sigle.rowChange.get("card_no").get)
      }
    }
    maps.toArray
  }

  def mapToEntity(rowMap: ArrayBuffer[scala.collection.mutable.LinkedHashMap[String, java.io.Serializable]], repository: ElasticsearchRepository[Object, String]): Any = {
    val json = objectMapper.writeValueAsString(rowMap)
    // 获得domain
    val cT = ClassTypeInformation.from(repository.getClass)
    // 解析获得domaiin
    val arguments = cT.getSuperTypeInformation(classOf[ElasticsearchRepository[_, _ <: Serializable]]).getTypeArguments
    //    logger.error("json="+json + " === " + arguments.get(0).getType);
    objectMapper.readValue(json, objectMapper.getTypeFactory.constructCollectionType(classOf[util.LinkedList[Object]], arguments.get(0).getType))
  }
}

case class EsDealDataRequest(repository: ElasticsearchRepository[Object, String], dataArray: Array[SchemaTableMapData], txId: String)

case class TransEsComplete(txId: String, isDbDeal: Boolean)