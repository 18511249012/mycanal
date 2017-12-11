package com.hzcard.syndata.datadeal

import java.util

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.DealDataStatus
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.data.util.ClassTypeInformation
import org.springframework.stereotype.Component

import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("esDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EsDealDataActor(@Autowired
                      applicationContext: ApplicationContext,@Autowired clientProperties:CanalClientProperties,
                     @Autowired @Qualifier("persistorLoaderRef") getNextActor:ActorRef) extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    if (!message.isEmpty && message.get.isInstanceOf[DealDataRequestModel]) { //遇到了异常，重启的时候，重复处理数据，需要注意死循环
      dealDataRequestModel(message.get.asInstanceOf[DealDataRequestModel])
    }
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

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
      val time = System.currentTimeMillis() - startTime
      if (log.isWarnEnabled && time > 1000)
        log.warn(s"操作es耗时 ${x.data._1._1}.${x.data._1._2} 耗时 ${time}")
    }
    sender ! DealDataStatus.Success
  } catch {
    case th: Throwable =>
      log.error(th.getMessage, th)
      throw th
  }

  def dealEs(x: EsDealDataRequest) = //处理下针对es的数组，将update操作当成insert操作合并
    try {
      val esChangeArray = x.dataArray.map(x => if (x.eventType == EventType.UPDATE) SchemaTableMapData(x.schema, x.tableName, EventType.INSERT, x.rowChange,null,x.primaryKeys) else x)
      val newArray = merger(esChangeArray)
      newArray.foreach(data => {
        val entitys = mapToEntity(data.get.rowChanges.toArray, x.repository).asInstanceOf[java.lang.Iterable[Object]]
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

  def mapToEntity(rowMap: Array[scala.collection.mutable.LinkedHashMap[String, java.io.Serializable]], repository: ElasticsearchRepository[Object, String]): Any = {
    val json = objectMapper.writeValueAsString(rowMap)
//    log.info(s"es syn json is :${json}")
    // 获得domain
    val cT = ClassTypeInformation.from(repository.getClass)
    // 解析获得domaiin
    val arguments = cT.getSuperTypeInformation(classOf[ElasticsearchRepository[_, _ <: Serializable]]).getTypeArguments
    objectMapper.readValue(json, objectMapper.getTypeFactory.constructCollectionType(classOf[util.LinkedList[Object]], arguments.get(0).getType))
  }
}

case class EsDealDataRequest(repository: ElasticsearchRepository[Object, String], dataArray: Array[SchemaTableMapData], txId: String)

case class TransEsComplete(txId: String, isDbDeal: Boolean)