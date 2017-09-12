package com.hzcard.syndata.extractlog.emitter

import akka.actor.{Actor, ActorLogging}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.config.autoconfig.MysqlClientProperties
import com.hzcard.syndata.datadeal.MergeOperationArray
import com.hzcard.syndata.extractlog.events.MutationWithInfo
import com.hzcard.syndata.send.MessagePool
import org.springframework.context.ApplicationContext


/**
  * Created by zhangwei on 2017/5/10.
  */
class EmitterActor(config: MysqlClientProperties, applicationContext: ApplicationContext) extends Actor with MergeOperationArray with RequiresMessageQueue[BoundedMessageQueueSemantics] with ActorLogging{

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  val messagePool = applicationContext.getBean("messagePool").asInstanceOf[MessagePool]

  override def receive: Receive = {
    case MutationWithInfo(mutation, t, _, mutationData, Some(message: String)) =>
      val adderSupplier = new java.util.function.BiFunction[String, SourceDataSourceChangeEvent, SourceDataSourceChangeEvent]() {  //同transaction的数据合并为一个transaction对象
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
      messagePool.compute(mutationData.transaction.id, adderSupplier)
      if (t.get.lastMutationInTransaction) {
        //事务结束，放入transactionId
        if(mutationData.transaction.last_mutation) {     //最后一行数据，表示放置transaction完毕
          messagePool.putTx(mutationData.transaction.id)
          log.info(s"Received transaction cacheMTId put  :${
            mutationData.transaction.id
          }")
        }
      }

    case _ =>
      throw new RuntimeException("message not match")
  }

}

case class MutationData(mutation: String, sequence: Long, database: String, table: String, query: QueryInfo, primary_key: collection.mutable.LinkedHashMap[String, java.io.Serializable], transaction: Transaction, row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable], old_row_data: collection.mutable.LinkedHashMap[String, java.io.Serializable])

case class QueryInfo(timestamp: Long, sql: String, row_count: Int, current_row: Int)

case class Transaction(id: String, last_mutation: Boolean, row_count: Option[Long] = None)

case class SourceDataSourceChangeEvent(txId: String, event: Array[MutationData], myChannel: String, binLogFileName: Option[String] = None, binLogPosition: Option[Long] = None)




