package com.hzcard.syndata.datadeal

import java.sql.SQLException

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import org.springframework.stereotype.Component

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhangwei on 2017/3/6.
  */
@Component("dbDealDataActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DbDealDataActor extends Actor with MergeOperationArray{

  override def receive: Receive = {
    case x: DealDataRequest => try {
      logger.error("DbDealDataActor dealData dataArray length is {},table is {}", x.dataArray.length, x.tableName)
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
      sender ! true
    }
    catch {
      case el: Throwable => {
        sender() ! akka.actor.Status.Failure(el)
        throw el
      }
    }
    case _ => sender ! NotMatchMessage
  }
}

case class DealDataRequest(override val mapDataSource: MapDataSourceLookup, schema: String, tableName: String, keyword: String, dataArray: Array[SchemaTableMapData]) extends ParentRequest(mapDataSource)
