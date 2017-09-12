package com.hzcard.syndata.datadeal

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.extractlog.emitter.SourceDataSourceChangeEvent

/**
  * Created by zhangwei on 2017/5/12.
  */

class HzcardDataDealActor(getNextHop: ActorRefFactory => ActorRef) extends Actor with ActorLogging with RequiresMessageQueue[BoundedMessageQueueSemantics]{
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)


  protected val nextHop = getNextHop(context)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage,reason)
    if(message.get.isInstanceOf[SourceDataSourceChangeEvent])
      dealSourceDataSourceChangeEvent(message.get.asInstanceOf[SourceDataSourceChangeEvent])
  }

  override def receive: Receive = {
    case d: SourceDataSourceChangeEvent => dealSourceDataSourceChangeEvent(d)
    case _ =>
      log.error(s"Received invalid message.")
      throw new RuntimeException("Received invalid message")
  }

  def dealSourceDataSourceChangeEvent(d:SourceDataSourceChangeEvent): Unit ={
    try {
      val datas = d.event.map(x => {
        SchemaTableMapData(x.database, x.table, EventType.valueOf(x.mutation.toUpperCase), x.row_data, x.transaction.id)
      }
      ).groupBy(s => (s.schema, s.tableName, s.transId)) //根据schema\tableName\txId分组
      datas.foreach(z => {
        nextHop ! DealDataRequestModel(z,d.txId)
      })
      nextHop ! BinLogPosition(d.myChannel, d.binLogFileName.get, d.binLogPosition.get, d.txId)
    } catch {
      case th: Throwable =>
        log.info(s"data is ${objectMapper.writeValueAsString(d)}")
        throw th
    }
  }

}

case class BinLogPosition(myChannel: String, binLogFileName: String, binLogPosition: Long, txId: String = null)

case class DealDataRequestModel(data:Tuple2[Tuple3[String,String,String],Array[SchemaTableMapData]],txId:String)

