package com.hzcard.syndata

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import com.hzcard.syndata.lister.DeadLettersActor
import com.hzcard.syndata.persist.PositionPersistActor
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, Configuration, DependsOn}

/**
  * Created by zhangwei on 2017/3/4.
  */
@Configuration
class ActorAutoConfig(@Autowired val applicationContext: ApplicationContext,
                      @Autowired canalClientProperties: CanalClientProperties, @Value("${app.name}") val appName: String) {

  var actorSystem: ActorSystem = _

  @Bean(name = Array("actorSystem"), destroyMethod = "terminate")
  def createActorSystem: ActorSystem = {
    actorSystem = ActorSystem.create("reorder-akka-" + appName)
//    SpringExtentionImpl(actorSystem)(applicationContext)
    val listener = actorSystem.actorOf(Props[DeadLettersActor])
    actorSystem.eventStream.subscribe(listener, classOf[DeadLetter])
    actorSystem
  }


  @Bean(name = Array("clusterActorRef"))
  @DependsOn(Array("actorSystem","transactionActorRef"))
  def clusterActor: ActorRef =
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("clusterActor"), "clusterActorRef")

  @Bean(name = Array("transactionActorRef"))
  @DependsOn(Array("actorSystem","columnInfoActorRef"))
  def transactionActorRef = actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("transactionActor"), "transactionActorRef")

  @Bean(name = Array("columnInfoActorRef"))
  @DependsOn(Array("actorSystem","jsonFormatterActorRef"))
  def columnInfoActorRef: ActorRef =
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("columnInfoActor"), "columnInfoActorRef")


  @Bean(name = Array("jsonFormatterActorRef"))
  @DependsOn(Array("actorSystem","emitterLoaderRef"))
  def jsonFormatterActorRef: ActorRef =
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("jsonFormatterActor"), "jsonFormatterActorRef")

  @Bean(name = Array("emitterLoaderRef"))
  @DependsOn(Array("actorSystem","hzcardDataDealActorRef"))
  def emitterLoaderRef =
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("emitterLoader"), "emitterLoaderRef")

  @Bean(name = Array("hzcardDataDealActorRef"))
  @DependsOn(Array("actorSystem","dbDealDataActorRef"))
  def hzcardDataDealActorRef =
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("hzcardDataDealActor"), "hzcardDataDealActorRef")

  @Bean(name = Array("dbDealDataActorRef"))
  @DependsOn(Array("actorSystem","esDealDataActorRef"))
  def dbDealDataActorRef = actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("dbDealDataActor"),"dbDealDataActorRef")


  @Bean(name = Array("esDealDataActorRef"))
  @DependsOn(Array("actorSystem","persistorLoaderRef"))
  def esDealDataActorRef=
    actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("esDealDataActor"),"esDealDataActorRef")

  @Bean(name = Array("persistorLoaderRef"))
  @DependsOn(Array("actorSystem","redisCache"))
  def persistorLoaderRef = actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("persistorLoader"),"persistorLoaderRef")

  //
  //    @Bean(name = Array("hzcardDataDealActorRef"))
  //    @DependsOn(Array("actorSystem"))
  //    def hzcardDetaDeal: ActorRef = {
  //      actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("hzcardDataDealActor").withDispatcher("my-dispatcher"), "hzcardDataDealActorRef")
  //    }
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
