package com.hzcard.syndata.extractlog.actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorRefFactory}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.extractlog.events._
import org.slf4j.LoggerFactory

class TransactionActor(getNextHop: ActorRefFactory => ActorRef) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {
  protected val log = LoggerFactory.getLogger(getClass)
  protected val nextHop = getNextHop(context)

  /** Mutable State! */
  protected var mutationCount: Long = 0
  protected var transactionInfo: Option[TransactionInfo] = None
  protected var previousMutation: Option[MutationWithInfo] = None
  private var binlogFilename: Option[String] = None

  protected def setState(
                          info: Option[TransactionInfo] = transactionInfo,
                          prev: Option[MutationWithInfo] = previousMutation
                        ) = {
    transactionInfo = info
    previousMutation = prev
  }

  def receive = {
    case BeginTransaction =>
      mutationCount = 0
      setState(info = Some(TransactionInfo(UUID.randomUUID.toString)), prev = None)
      if (log.isInfoEnabled)
        log.info(s"Received BeginTransacton txId = ${transactionInfo.get.gtid}")

    case rotate: RotateEvent =>
      if (log.isInfoEnabled)
        log.info(s"RotateEvent ,binlogFilename is ${rotate.binlogFilename}")
      binlogFilename = Some(rotate.binlogFilename) //日志滚动事件

    case Gtid(guid) =>
      if (log.isDebugEnabled)
        log.debug(s"Received GTID for transaction: ${guid}")
      setState(info = Some(TransactionInfo(guid)))

    case x: CommitTransaction =>
      if (transactionInfo.isEmpty)
        log.warn(s"CommitTransaction event not transaction info binlogFilename=${binlogFilename} , positon ${x.binlogPosition}")
      if (log.isInfoEnabled)
        log.info(s"Received Commit/Rollback txId is ${transactionInfo.get.gtid}")
      previousMutation.foreach { mutation =>
        nextHop ! mutation.copy(transaction = transactionInfo.map { info =>
          info.copy(positionInfo = Some(PositionInfo(binlogFilename.getOrElse(""), x.binlogPosition)), rowCount = mutationCount, lastMutationInTransaction = true)
        })
      }
      setState(info = None, prev = None)
//      TimeUnit.MILLISECONDS.sleep(10L)

    case event: MutationWithInfo =>
      transactionInfo match {
        case None =>
          mutationCount = 1L
          val copyEvent = event.copy(
            transaction =
              Some(TransactionInfo(UUID.randomUUID.toString, positionInfo = Some(PositionInfo(binlogFilename.getOrElse(""), event.mutation.binLogPositon)), rowCount = mutationCount, lastMutationInTransaction = true)
              )
          )
          nextHop ! copyEvent
          if (log.isInfoEnabled)
            log.info(s"transactionInfo None Received generate txId: ${copyEvent.transaction.get.gtid}")
        case Some(info) =>
          previousMutation.foreach { mutation =>
            nextHop ! mutation.copy(transaction = transactionInfo.map { info =>
              info.copy(positionInfo = Some(PositionInfo(binlogFilename.getOrElse(""), event.mutation.binLogPositon)))
            })
          }
          mutationCount += event.mutation.rows.length;
          setState(prev = Some(event))
      }
    case _ =>
      throw new Exception("Invalid message received by TransactionActor")
  }
}
