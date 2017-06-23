package com.hzcard.syndata

import akka.actor.{ActorRef, ActorSystem}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, Configuration, DependsOn}

/**
  * Created by zhangwei on 2017/3/4.
  */
@Configuration
class ActorAutoConfig(@Autowired val applicationContext: ApplicationContext) {
  private var actorSystem: ActorSystem = _

  @Bean(name = Array("actorSystem"), destroyMethod = "terminate")
  def createActorSystem: ActorSystem = {
    actorSystem = ActorSystem.create("dataDeal-akka")
    SpringExtentionImpl(actorSystem)(applicationContext)
    actorSystem
  }

  @Bean(name = Array("hzcardDataDealActorRef"))
  @DependsOn(Array("actorSystem"))
  def hzcardDetaDeal: ActorRef = {
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("hzcardDataDealActor"), "hzcardDataDealActor")
  }
}
