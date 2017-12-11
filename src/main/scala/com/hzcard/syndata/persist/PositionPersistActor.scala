package com.hzcard.syndata.persist

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorLogging, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import com.hzcard.syndata.datadeal.BinLogPosition
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.collection.mutable


@Component("persistorLoader")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PositionPersistActor(@Autowired redisCache: RedisCache,@Autowired clientProperties:CanalClientProperties) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {


  val log = LoggerFactory.getLogger(getClass)

  val positionCache = new mutable.HashMap[String, Int] //记录transaction的数量，不至于频繁写redis

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    if (!message.isEmpty && message.get.isInstanceOf[BinLogPosition]) { //遇到了异常，重启的时候，重复处理数据，需要注意死循环
      redisCache.savePosition(message.get.asInstanceOf[BinLogPosition])
    }
  }

  override def receive: Receive = {
    case x: BinLogPosition =>
      if (positionCache.get(x.myChannel).getOrElse(0) == 0)
        positionCache.put(x.myChannel, 1)
      else
        positionCache.put(x.myChannel, positionCache.get(x.myChannel).get + 1)
      if (positionCache.get(x.myChannel).get >= clientProperties.getTransCacheCount) {
        log.warn(s"txId is ${x.txId} ,binpositon saved ${x.myChannel},${x.binLogFileName},${x.binLogPosition}")
        redisCache.savePosition(x) //将位点做持久化
        positionCache.put(x.myChannel, 0) //重置缓存数量
      }
    case _ => {
      throw new RuntimeException("message not match")
    }
  }
}
