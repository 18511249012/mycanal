package com.hzcard.syndata.extractlog.emitter

import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap, TimeUnit}

import akka.actor.{Actor, ActorRef}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.pattern.ask
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.config.autoconfig.MysqlClientProperties
import com.hzcard.syndata.extractlog.events.MutationWithInfo
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/5/10.
  */
class EmitterActor(config: MysqlClientProperties, applicationContext: ApplicationContext) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {
  implicit val timeout = Timeout(60 minutes)
  val log = LoggerFactory.getLogger(getClass)
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  val cacheMTId = new ArrayBlockingQueue[String](64) //最多缓存64条事务
  val cacheMessage = new ConcurrentHashMap[String, SourceDataSourceChangeEvent]
  var isSend = true
  val bufferSize = 4L
  val hzcardDataDealActor = applicationContext.getBean("hzcardDataDealActorRef").asInstanceOf[ActorRef]

  val sendMessage = new Thread() {
    override def run(): Unit = {
      val toSendMessages = new ArrayBuffer[SourceDataSourceChangeEvent](0)
      while (isSend) {
        var isPut = true
        var isSleeped = false
        while (isPut) {
          if (toSendMessages.size == bufferSize) {
            log.warn(s"cacheMessage is full")
            isPut = false
          } else {
            if (isSleeped)
              isPut = false
            val txId = cacheMTId.take()
            if (txId != null) {
              val signleMessage = cacheMessage.get(txId)
              if (signleMessage != null) {
                toSendMessages += signleMessage
                cacheMessage.remove(txId)
              }
              else
                log.warn(s"cacheMTId  ${txId} , cacheMessage is null")
            } else if (!isSleeped) {
              try {
                TimeUnit.MILLISECONDS.sleep(1L)
                isSleeped = true
              } catch {
                case ie: InterruptedException =>
              }
            }
          }
        }
        if (toSendMessages.length > 0) {
          val future = hzcardDataDealActor ? (toSendMessages.toArray)
          try {
            val isDeal = Await.result(future, timeout.duration).asInstanceOf[Boolean]
            if (isDeal) {
              toSendMessages.clear()
            }
          } catch {
            case x: Throwable =>
              log.info(s"fail data send array size is ${toSendMessages.length}")
              log.error("处理数据失败！，重新发送", x)
          }
        }
        else
          TimeUnit.MICROSECONDS.sleep(10)

      }
    }
  }
  sendMessage.start()

  override def receive: Receive = {
    case MutationWithInfo(mutation, t, _, mutationData, Some(message: String)) =>
      val adderSupplier = new java.util.function.BiFunction[String, SourceDataSourceChangeEvent, SourceDataSourceChangeEvent]() {
        override def apply(k: String, u: SourceDataSourceChangeEvent): SourceDataSourceChangeEvent = {
          if (u == null) {
            if (t.get.lastMutationInTransaction)
              SourceDataSourceChangeEvent(t.get.gtid, Array(mutationData), config.getMyChannel, Some(t.get.positionInfo.get.binLogFilename), Some(t.get.positionInfo.get.binLogPositon))
            else
              SourceDataSourceChangeEvent(t.get.gtid, Array(mutationData), config.getMyChannel)
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
      if (t.get.lastMutationInTransaction) { //事务结束，放入transactionId
        cacheMTId.offer(mutationData.transaction.id)
      }
    //      TimeUnit.MILLISECONDS.sleep(10L)
    case _ =>
      log.error(s"Received invalid message.")
      sender() ! akka.actor.Status.Failure(new Exception("Received invalid message"))
  }

  override def postStop(): Unit = {
    isSend = false
    TimeUnit.SECONDS.sleep(5L)
  }
}

case class MutationData(mutation: String, sequence: Long, database: String, table: String, query: QueryInfo, primary_key: collection.mutable.LinkedHashMap[String, java.io.Serializable], transaction: Transaction, row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable], old_row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable])

case class QueryInfo(timestamp: Long, sql: String, row_count: Int, current_row: Int)

case class Transaction(id: String, last_mutation: Boolean, row_count: Option[Long] = None)

case class SourceDataSourceChangeEvent(txId: String, event: Array[MutationData], myChannel: String, binLogFileName: Option[String] = None, binLogPosition: Option[Long] = None)




