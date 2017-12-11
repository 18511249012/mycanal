package com.hzcard.syndata.extractlog.actors

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.extractlog.events._
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.collection.mutable

@Component("transactionActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TransactionActor(@Autowired @Qualifier("columnInfoActorRef") nextHop:ActorRef) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  /** Mutable State! */
  protected var mutationCount: mutable.HashMap[String,Long] = new mutable.HashMap[String,Long]()
  protected var transactionInfo: mutable.HashMap[String, TransactionInfo] = new mutable.HashMap[String, TransactionInfo]()
  protected var previousMutation: mutable.HashMap[String, MutationWithInfo] = new mutable.HashMap[String, MutationWithInfo]()
  private var binlogFilename: mutable.HashMap[String, String] = new mutable.HashMap[String, String]()

  protected def setState(
                          myChannel: String,
                          info: Option[TransactionInfo],
                          prev: Option[MutationWithInfo]
                        ) = {
    if (!info.isEmpty)
      transactionInfo.put(myChannel, info.get)
    else
      transactionInfo.remove(myChannel)
    if (!prev.isEmpty)
      previousMutation.put(myChannel, prev.get)
    else
      previousMutation.remove(myChannel)

  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  def receive = {
    case x:BeginTransactionWithChannel =>
      mutationCount.put(x.mychannel,0L)
      setState(x.mychannel, info = Some(TransactionInfo(UUID.randomUUID.toString)), prev = None)
      if (log.isDebugEnabled)
        log.debug(s"Received BeginTransacton channel is ${x.mychannel}, txId = ${transactionInfo.get(x.mychannel).get.gtid}")

    case rotate: RotateEvent =>
      if (log.isDebugEnabled)
        log.debug(s"RotateEvent ,binlogFilename is ${rotate.binlogFilename}")
      //日志滚动事件
      binlogFilename.put(rotate.myChannel.get, rotate.binlogFilename)
    case x: CommitTransaction =>
      if (transactionInfo.isEmpty)
        log.warn(s"CommitTransaction event not transaction info binlogFilename=${binlogFilename.get(x.myChannel.get)} , positon ${x.binlogPosition}")
      if (log.isDebugEnabled)
        log.debug(s"Received Commit/Rollback txId is ${transactionInfo.get(x.myChannel.get).get.gtid}")
      previousMutation.get(x.myChannel.get).foreach { mutation =>
        nextHop ! mutation.copy(transaction = transactionInfo.get(x.myChannel.get).map { info =>
          info.copy(positionInfo = Some(PositionInfo(binlogFilename.get(x.myChannel.get).getOrElse(""), x.binlogPosition)), rowCount = mutationCount.get(x.myChannel.get).getOrElse(0L), lastMutationInTransaction = true)
        },myChannel = x.myChannel)
      }
      setState(x.myChannel.get, info = None, prev = None)
    case event: MutationWithInfo =>
      transactionInfo.get(event.myChannel.get)
      match {
        case None =>
          mutationCount.put(event.myChannel.get,1L)
          val copyEvent = event.copy(
            transaction =
              Some(TransactionInfo(UUID.randomUUID.toString, positionInfo = Some(PositionInfo(binlogFilename.get(event.myChannel.get).getOrElse(""), event.mutation.binLogPositon)), rowCount = mutationCount.get(event.myChannel.get).getOrElse(0L), lastMutationInTransaction = true)
              )
          ,myChannel = event.myChannel)
          nextHop ! copyEvent
          if (log.isDebugEnabled)
            log.debug(s"transactionInfo None Received generate txId: ${copyEvent.transaction.get.gtid}")
        case Some(info) =>
          previousMutation.get(event.myChannel.get).foreach { mutation =>
            nextHop ! mutation.copy(transaction = transactionInfo.get(event.myChannel.get).map { info =>
              info.copy(positionInfo = Some(PositionInfo(binlogFilename.get(event.myChannel.get).getOrElse(""), event.mutation.binLogPositon)))
            },myChannel = event.myChannel)
          }
          mutationCount.put(event.myChannel.get,mutationCount.get(event.myChannel.get).get + event.mutation.rows.length)
          setState(event.myChannel.get, transactionInfo.get(event.myChannel.get), prev = Some(event))
      }
    case x: AlterTableEvent => nextHop ! x
    case x: akka.actor.Status.Failure => log.error("failure exception ",x.cause)
    case othe =>
      throw new Exception(s"Invalid message received by TransactionActor , message: ${othe}")
  }

}
