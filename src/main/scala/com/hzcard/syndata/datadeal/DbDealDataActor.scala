package com.hzcard.syndata.datadeal

import java.sql.SQLException

import akka.actor.Actor
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.{Lazy, Scope}
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import org.springframework.stereotype.Component

/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("dbDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy(false)
class DbDealDataActor extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics]{

//  val logger = LoggerFactory.getLogger(getClass)
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  override def receive: Receive = {
    case x: DealDataRequest => try {
      val start = System.currentTimeMillis()
      logger.info(s" start DbDealDataActor data size is ${x.dataArray.size}")
      val newArray = merger(x.dataArray)
      newArray.foreach(data => {
        if (data.get.eventType == EventType.INSERT)
          try {
            DBOperationScala(x.mapDataSource).save(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)
          } catch {
            case e: SQLException => {
              if (e.getMessage.indexOf("Duplicate entry") < 0)
                throw e
            }
          }
        else if (data.get.eventType == EventType.UPDATE)
          DBOperationScala(x.mapDataSource).update(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)
        else
          DBOperationScala(x.mapDataSource).delete(x.schema, x.tableName, x.keyword, data.get.rowChanges.toArray)
      })
      val time = System.currentTimeMillis()-start
      logger.info(s"end DbDealDataActor ,time ${time}" )
      if(time>1000L)
        logger.warn(s"sql execution time to long  ${time}, message = ${objectMapper.writeValueAsString(x)}")
      sender ! true
    }
    catch {
      case el: Throwable => {
        logger.error(s"dbDealDataActor exception ${el.getMessage}",el)
        sender() ! akka.actor.Status.Failure(el)
        throw el
      }
    }
    case _ => sender ! NotMatchMessage
  }
}

case class DealDataRequest(override val mapDataSource: MapDataSourceLookup, schema: String, tableName: String, keyword: String, dataArray: Array[SchemaTableMapData]) extends ParentRequest(mapDataSource)
