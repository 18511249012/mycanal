package com.hzcard.syndata.datadeal

import java.util.concurrent.ConcurrentHashMap

import akka.actor.Status.{Failure, Status, Success}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, ActorRefFactory, ActorSystem, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.{DealDataStatus, SpringExtentionImpl}
import com.hzcard.syndata.extractlog.emitter.SourceDataSourceChangeEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/5/12.
  */
@Component("hzcardDataDealActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class HzcardDataDealActor(@Autowired objectMapper: ObjectMapper, @Autowired @Qualifier("dbDealDataActorRef") nextHop: ActorRef) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  implicit val timeout = Timeout(60 seconds)


  val failPosition = new ConcurrentHashMap[String, BinLogPosition]

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    if (message.get.isInstanceOf[SourceDataSourceChangeEvent])
      dealSourceDataSourceChangeEvent(message.get.asInstanceOf[SourceDataSourceChangeEvent])
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  override def preStart(): Unit = {

    super.preStart()
  }

  override def receive: Receive = {
    case d: SourceDataSourceChangeEvent => dealSourceDataSourceChangeEvent(d)
    case othe =>
      log.error(s"Received invalid message.,message is ${othe}")
      throw new RuntimeException("Received invalid message")
  }

  def dealSourceDataSourceChangeEvent(d: SourceDataSourceChangeEvent): Unit = {
    try {
      var isSuccess = false
      while (!isSuccess) { //这里会不断重试，以保证消息不会因为重启而丢失
        val datas = d.event.map(x => {
          SchemaTableMapData(x.database, x.table, EventType.valueOf(x.mutation.toUpperCase), x.row_data, x.transaction.id, x.primary_key,x.columnTypes)
        }
        ).groupBy(s => (s.schema, s.tableName, s.transId)) //根据schema\tableName\txId分组

        try {
          val dealResult = datas.map(z => {
            nextHop ? DealDataRequestModel(z, d.txId)
          })
          dealResult.foreach(resultF => {
            Await.result(resultF, timeout.duration).asInstanceOf[DealDataStatus]
          })
          isSuccess = true
        } catch {
          case x: Throwable =>
            //记录失败的位点
            log.error(s"hzcard deal data exception ${x.getMessage},retry deal tx is ${d.txId}", x)
        }
      }
      if (!d.binLogFileName.isEmpty && !d.binLogPosition.isEmpty)
        nextHop ! BinLogPosition(d.myChannel, d.binLogFileName.get, d.binLogPosition.get, d.txId)
      sender ! DealDataStatus.Success

    } catch {
      case th: Throwable =>
        log.error(s"hzcard deal error data is ${objectMapper.writeValueAsString(d)}", th)
        throw th
    }
  }

  def getLteePotion(a: BinLogPosition, b: BinLogPosition): BinLogPosition = {
    val compartName = a.binLogFileName.compareTo(b.binLogFileName)
    if (compartName == 0) {
      if (a.binLogPosition.compareTo(b.binLogPosition) <= 0)
        a
      else
        b
    } else {
      if (compartName <= 0)
        a
      else
        b
    }

  }


}

case class BinLogPosition(myChannel: String, binLogFileName: String, binLogPosition: Long, txId: String = null)


case class DealDataRequestModel(data: Tuple2[Tuple3[String, String, String], Array[SchemaTableMapData]], txId: String)

