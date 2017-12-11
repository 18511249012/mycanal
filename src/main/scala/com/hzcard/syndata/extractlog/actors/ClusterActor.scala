package com.hzcard.syndata.extractlog.actors

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, ActorRef, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.extractlog.events._
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
  * Created by zhangwei on 2017/6/26.
  */
@Component("clusterActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ClusterActor(@Autowired redis: RedisCache,@Autowired @Qualifier("transactionActorRef") nextHop:ActorRef) extends Actor
  with RequiresMessageQueue[BoundedMessageQueueSemantics]  {

  val log = LoggerFactory.getLogger(getClass)

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    self ! message //重试
  }

  override def receive: Receive = {
    case x: HeartSend => {
      if (log.isDebugEnabled)
        log.debug(s"ClusterActor receive heardSend channel is ${x.myChannel},event is ${x.event}")
      x.event match {
        case y:RotateEvent => {
          if (log.isInfoEnabled)
            log.info(s"ClusterActor send ${nextHop} RotateEvent  channel ${x.myChannel},RotateEvent binlog is ${y.binlogFilename}.${y.binlogPosition}")
          nextHop ! y.copy(myChannel = Some(x.myChannel))
        }
        case BeginTransaction  if(redis.clusterIsAlive(x.myChannel)) => nextHop ! BeginTransactionWithChannel(x.myChannel)
        case y:CommitTransaction if(redis.clusterIsAlive(x.myChannel)) => nextHop ! y.copy(myChannel = Some(x.myChannel))
        case y:MutationWithInfo  if(redis.clusterIsAlive(x.myChannel)) => nextHop ! y.copy(myChannel = Some(x.myChannel))
        case unKonwn => log.error(s"ClusterActor match event ,event is ${unKonwn}")
      }
    }
    case x: AlterTableEvent => nextHop ! x
    case x: MutationWithInfo => nextHop ! x         //用于重发使用，不要被其他actor调用
    case x: BeginTransactionWithChannel => nextHop ! x      //重试
    case x: CommitTransaction => nextHop ! x       //重试
    case x: RotateEvent => nextHop ! x         //重试
    case _ => throw new RuntimeException("message not match!!!")
  }
}

case class HeartSend(myChannel: String, event: Any)

case class HeartDestory(myChannel: String)

case class BeginTransactionWithChannel(mychannel:String)
