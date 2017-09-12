package com.hzcard.syndata

import akka.actor.{ActorRef, ActorSystem}
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, Configuration, DependsOn}

/**
  * Created by zhangwei on 2017/3/4.
  */
@Configuration
class ActorAutoConfig(@Autowired val applicationContext: ApplicationContext, @Autowired canalClientProperties: CanalClientProperties) {


  import collection.JavaConverters._

  var actorSystem :ActorSystem=_

  @Bean(name = Array("actorSystem"), destroyMethod = "terminate")
  def createActorSystem: ActorSystem = {
    actorSystem = ActorSystem.create("reorder-akka")
    SpringExtentionImpl(actorSystem)(applicationContext)
    actorSystem
  }


  //
  //  @Bean(name = Array("hzcardDataDealActorRef"))
  //  @DependsOn(Array("actorSystem"))
  //  def hzcardDetaDeal: ActorRef = {
  //    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("hzcardDataDealActor").withDispatcher("my-dispatcher"), "hzcardDataDealActorRef")
  //  }
  //
  //  @Bean(name = Array("dbDealDataActorRef"))
  //  @DependsOn(Array("actorSystem"))
  //  def dbDealDataActorRef: ActorRef = {
  //    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("dbDealDataActor").withDispatcher("my-dispatcher"), "dbDealDataActorRef")
  //  }
  //
  //  @Bean(name = Array("esDealDataActorRef"))
  //  @DependsOn(Array("actorSystem"))
  //  def esDealDataActorRef: ActorRef = {
  //    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("esDealDataActor").withDispatcher("my-dispatcher"), "esDealDataActorRef")
  //  }
  //
    /*@Bean(name = Array("clusterActorRef"))
    @DependsOn(Array("actorSystem"))
    def clusterActorRef: ActorRef = {
      actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("clusterActor").withDispatcher("my-dispatcher"), "clusterActorRef")
    }*/
  //
  //  @Bean(name = Array("positionPersistActorRef"))
  //  @DependsOn(Array("actorSystem"))
  //  def positionPersistActorRef:ActorRef ={
  //    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("PositionPersistActor").withDispatcher("my-dispatcher"), "positionPersistActorRef")
  //  }


}
