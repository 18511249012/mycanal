package com.hzcard.syndata.extractlog.actors

import akka.actor.Actor
import com.hzcard.syndata.redis.RedisCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
  * Created by zhangwei on 2017/6/26.
  */
@Component("clusterActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ClusterActor(@Autowired val redis: RedisCache) extends Actor{
  override def receive: Receive = {
    case x:HeartSend => redis.keepAlive(x.myChannel)
    case x:HeartDestory => redis.unRegiestSelfRunnerServer(x.myChannel)
  }
}

case class HeartSend(myChannel:String)

case class HeartDestory(myChannel:String)
