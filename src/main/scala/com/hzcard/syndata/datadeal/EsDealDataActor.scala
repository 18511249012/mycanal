package com.hzcard.syndata.datadeal

import java.util

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.util.ClassTypeInformation
import org.springframework.stereotype.{Component, Repository}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("esDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EsDealDataActor extends Actor with MergeOperationArray{


  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  override def receive: Receive = {
    case x: EsDealDataRequest => try {
      logger.error("EsDealDataActor dealData dataArray length is {}", x.dataArray.length)
      //处理下针对es的数组，将update操作当成insert操作合并
      val esChangeArray = x.dataArray.map(x => if (x.eventType == EventType.UPDATE) SchemaTableMapData(x.schema, x.tableName, EventType.INSERT, x.rowChange) else x)
      val newArray = merger(esChangeArray)
      newArray.foreach(data => {
//        val entitys = mapToEntity(data.get.rowChanges, x.repository).asInstanceOf[util.LinkedList[Object]]
        val entitys = mapToEntity(data.get.rowChanges, x.repository).asInstanceOf[java.lang.Iterable[Object]]
        if (data.get.eventType == EventType.DELETE)
          x.repository.delete(entitys)
        else
          x.repository.save(entitys)
      })
      sender ! true
    } catch {
      case e: Throwable =>
        sender ! akka.actor.Status.Failure(e)
        throw e
    }
    case _ => sender ! NotMatchMessage

  }

  def mapToEntity(rowMap: ArrayBuffer[scala.collection.mutable.LinkedHashMap[String, Object]], repository: ElasticsearchRepository[Object, String]): Any = {
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
