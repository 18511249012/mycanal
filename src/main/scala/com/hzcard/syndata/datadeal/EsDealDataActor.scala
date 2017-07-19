package com.hzcard.syndata.datadeal

import java.util

import akka.actor.Actor
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.pattern.pipe
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.data.util.ClassTypeInformation
import org.springframework.stereotype.Component

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("esDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EsDealDataActor extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics] {


  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  import context.dispatcher
  override def receive: Receive = {
    case x: EsDealDataRequest => try {
      //处理下针对es的数组，将update操作当成insert操作合并
      val start = System.currentTimeMillis()
      Future {
        logger.info(s" start esDealDataActor data size is ${x.dataArray.size}")
        val esChangeArray = x.dataArray.map(x => if (x.eventType == EventType.UPDATE) SchemaTableMapData(x.schema, x.tableName, EventType.INSERT, x.rowChange) else x)
        val newArray = merger(esChangeArray)
        newArray.foreach(data => {
          val entitys = mapToEntity(data.get.rowChanges, x.repository).asInstanceOf[java.lang.Iterable[Object]]
          if (data.get.eventType == EventType.DELETE)
            x.repository.delete(entitys)
          else
            x.repository.save(entitys)
        })
        val time = System.currentTimeMillis() - start
        logger.info(s"end esDealDataActor ,time ${time}")
         true
      } pipeTo sender
    } catch {
      case e: Throwable =>
        logger.error(s"EsDealDataActor exception ${e.getMessage}", e)
        sender ! akka.actor.Status.Failure(e)
        throw e
    }
    case _ => sender ! NotMatchMessage

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

case class EsDealDataRequest(repository: ElasticsearchRepository[Object, String], dataArray: Array[SchemaTableMapData])
