package com.hzcard.syndata

import java.util
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.{PostConstruct, PreDestroy}

import akka.actor.{ActorRef, ActorSystem}
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import com.hzcard.syndata.extractlog.Slaver
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{DependsOn, Scope}
import org.springframework.stereotype.Component
import org.springframework.util.{Assert, ClassUtils}

/**
  * Created by zhangwei on 2017/7/18.
  */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@DependsOn(Array("actorSystem", "redisCache","mapDataSource","clusterActorRef"))
class CanalClientContext(@Autowired redis: RedisCache, @Autowired actorSystem: ActorSystem,
                         @Autowired canalClientProperties: CanalClientProperties,
                         @Autowired applicationContext: ApplicationContext) {
  private val logger = LoggerFactory.getLogger(classOf[CanalClientContext])

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  private val resetEndPoint: Map[String, Class[_]] = new ConcurrentHashMap[String, Class[_]]
  private val clients: Map[String, Slaver] = new util.HashMap[String, Slaver]

  validate(canalClientProperties)
  canalClientProperties.getSchemas.asScala.filter(x => x._2 != null).
    filter(x => x._2.getTableRepository != null).
    map(x => x._2.getTableRepository).
    flatMap(x => x.values).
    filter(x => x.getRepository != null).
    foreach(x => {
      resetEndPoint.computeIfAbsent(x.getResetEndPoint, new java.util.function.Function[String, Class[_]] {
        override def apply(t: String): Class[_] = ClassUtils.forName(x.getRepository, applicationContext.getClassLoader)
      })
    })
  canalClientProperties.getDestinations.asScala.
    foreach(x => clients.put(x._1, new Slaver(x._2, actorSystem, applicationContext, redis)))
  start()

  @PreDestroy
  def cleanUp(): Unit = {
    System.out.println("canalclientContext destroy")
    import scala.collection.JavaConversions._
    for (client <- clients.values) {
      client.disconnect
    }
  }

  def validate(canalClientProperties: CanalClientProperties) = {
    Assert.notNull(canalClientProperties.getDestinations, "Destinations must not null")
    Assert.notEmpty(canalClientProperties.getSchemas, "table Repository must not null")
  }

  def getRepositoryByEndPoint(reporsitory: String) = {
    resetEndPoint.get(reporsitory)
  }

  def start(): Unit = {
    clients.asScala.foreach(x => x._2.connect())
  }

}
