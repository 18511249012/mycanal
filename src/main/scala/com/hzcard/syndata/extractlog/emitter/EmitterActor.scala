package com.hzcard.syndata.extractlog.emitter

import java.util.concurrent.ConcurrentHashMap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, ActorRefFactory, ActorSystem, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.{DealDataStatus, SpringExtentionImpl}
import com.hzcard.syndata.config.autoconfig.MysqlClientProperties
import com.hzcard.syndata.datadeal.{DealDataRequestModel, MergeOperationArray}
import com.hzcard.syndata.extractlog.events.{Column, MutationWithInfo}
import com.hzcard.syndata.send.MessagePool
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
  * Created by zhangwei on 2017/5/10.
  */
@Component("emitterLoader")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EmitterActor(@Autowired @Qualifier("hzcardDataDealActorRef") nextHop: ActorRef, @Autowired objectMapper: ObjectMapper) extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  //  val messagePool = applicationContext.getBean("messagePool").asInstanceOf[MessagePool]
  implicit val timeout = Timeout(60 seconds)

  val cacheMessage = new ConcurrentHashMap[String, SourceDataSourceChangeEvent]

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    if (!message.isEmpty && message.get.isInstanceOf[MutationWithInfo]) { //遇到了异常，重启的时候，重复发送
      self ! message.get
    }
  }

  override def receive: Receive = {
    case MutationWithInfo(mutation, t, columns, mutationData, Some(message: String), myChannel) =>
      val adderSupplier = new java.util.function.BiFunction[String, SourceDataSourceChangeEvent, SourceDataSourceChangeEvent]() { //同transaction的数据合并为一个transaction对象
        override def apply(k: String, u: SourceDataSourceChangeEvent): SourceDataSourceChangeEvent = {
          if (u == null) {
            if (t.get.lastMutationInTransaction)
              SourceDataSourceChangeEvent(t.get.gtid, Array(mutationData), myChannel.get, Some(t.get.positionInfo.get.binLogFilename), Some(t.get.positionInfo.get.binLogPositon))
            else
              SourceDataSourceChangeEvent(t.get.gtid, Array(mutationData), myChannel.get)
          } else {
            val newEvent = u.event ++ Array(mutationData)
            if (t.get.lastMutationInTransaction)
              u.copy(event = newEvent, binLogFileName = Some(t.get.positionInfo.get.binLogFilename), binLogPosition = Some(t.get.positionInfo.get.binLogPositon))
            else
              u.copy(event = newEvent)
          }
        }
      }
      cacheMessage.compute(mutationData.transaction.id, adderSupplier)
      if (t.get.lastMutationInTransaction) {
        //事务结束，放入transactionId
        if (mutationData.transaction.last_mutation) { //最后一行数据，表示放置transaction完毕
          Await.result(nextHop ? cacheMessage.remove(mutationData.transaction.id), timeout.duration).asInstanceOf[DealDataStatus]
        }
      }
    case othe =>
      throw new RuntimeException(s"message not match,message is ${othe}")
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }



}

case class MutationData(mutation: String, sequence: Long, database: String, table: String, query: QueryInfo, primary_key: collection.mutable.LinkedHashMap[String, java.io.Serializable], transaction: Transaction, row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable], old_row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable], columnTypes: collection.mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty[String, String])

case class QueryInfo(timestamp: Long, sql: String, row_count: Int, current_row: Int)

case class Transaction(id: String, last_mutation: Boolean, row_count: Option[Long] = None)

case class SourceDataSourceChangeEvent(txId: String, event: Array[MutationData], myChannel: String, binLogFileName: Option[String] = None, binLogPosition: Option[Long] = None)




