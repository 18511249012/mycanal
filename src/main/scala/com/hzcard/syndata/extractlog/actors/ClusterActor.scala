package com.hzcard.syndata.extractlog.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.extractlog.events.{ RotateEvent}
import com.hzcard.syndata.redis.RedisCache
import org.springframework.context.ApplicationContext

import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/6/26.
  */
class ClusterActor(getNextHop: (ActorRefFactory) => ActorRef, applicationContext: ApplicationContext) extends Actor
  with RequiresMessageQueue[BoundedMessageQueueSemantics] with ActorLogging {
  val redis = applicationContext.getBean(classOf[RedisCache])
  protected val nextHop = getNextHop(context)


  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    self ! message //重试
  }

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: Exception => Restart
  }

  override def receive: Receive = {
    case x: HeartSend => {
      if (redis.clusterIsAlive(x.myChannel)) //如果未false，其他节点在处理，消息抛弃
        nextHop ! x.event
      else
        if(x.event.isInstanceOf[RotateEvent]){
//        log.error("test mail send")
          nextHop ! x.event //需要把binlogFileName传入actor，否则会为空，和记录的bingFileName是错误的
        }
    }
    case _ => throw new RuntimeException("message not match!!!")
  }
}

case class HeartSend(myChannel: String, event: Any)

case class HeartDestory(myChannel: String)
