package com.hzcard.syndata.extractlog

import java.io.IOException

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode
import com.hzcard.syndata.config.autoconfig.{DestinationProperty, Encryptor}
import com.hzcard.syndata.datadeal.BinLogPosition
import com.hzcard.syndata.extractlog.actors.{HeartDestory, HeartSend}
import com.hzcard.syndata.redis.RedisCache
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/4/27.
  * 看构造函数里的connect()
  */
class Slaver(clientProperties: DestinationProperty, system: ActorSystem, applicationContext: ApplicationContext, redis: RedisCache) {

  val logger = LoggerFactory.getLogger(classOf[Slaver])
  //  protected implicit val ec = system.dispatcher

  protected var isPaused = false

  protected lazy val client = new BinaryLogClient(
    clientProperties.getMysql.getHost,
    clientProperties.getMysql.getPort,
    clientProperties.getMysql.getUser,
    clientProperties.getMysql.getPassword
  )

  client.setServerId(System.currentTimeMillis())

  /** If we lose the connection to the server retry every `changestream.mysql.keepalive` milliseconds. **/
  client.setKeepAliveInterval(clientProperties.getMysql.getKeepalive)

  client.setConnectTimeout(clientProperties.getMysql.getTimeout)

  /** Register the objects that will receive `onEvent` calls and deserialize data **/
  client.registerEventListener(ChangeStreamEventListener(system, applicationContext, clientProperties))
  client.setEventDeserializer(new ChangestreamEventDeserializer)

  /** Register the object that will receive BinaryLogClient connection lifecycle events **/
  client.registerLifecycleListener(ChangeStreamLifecycleListener(clientProperties.getMysql.getMyChannel, redis))

  //  val clusterActorRef = applicationContext.getBean("clusterActorRef").asInstanceOf[ActorRef]

  protected def getConnected = {
    /** Finally, signal the BinaryLogClient to start processing events **/
    //    monitor.
    if (logger.isDebugEnabled)
      logger.debug(s"Starting ExtractBinLog...")
    //    redis.regiestRunnerServer(clientProperties.getMysql.getMyChannel)
    //    clusterActorRefSchedel = Some(system.scheduler.schedule(1 minutes, 2 minutes, clusterActorRef, HeartSend(clientProperties.getMysql.getMyChannel))) //每2分钟发送一次我还活着的消息

    val position = redis.getPosition(clientProperties.getMysql.getMyChannel).getOrElse(BinLogPosition(clientProperties.getMysql.getMyChannel, null, 0L))

    if (!StringUtils.isEmpty(position.binLogFileName)) {
      logger.info(s"Starting ExtractBinLog... binLogFileName is ${position.binLogFileName},binLogPosition is ${position.binLogPosition}")
      client.setBinlogFilename(position.binLogFileName)
      client.setBinlogPosition(position.binLogPosition)
    }

    while (!isPaused && !client.isConnected) {
      try {
        //        logger.info(s"Starting client connect")
        client.connect()
      }
      catch {
        case e: IOException =>
          logger.error(e.getMessage)
          logger.error("Failed to connect to MySQL to stream the binlog, retrying...", e)
          Thread.sleep(5000)
        case e: IllegalStateException =>
          logger.warn(s"${e.getMessage}", e)
        case e: Exception =>
          logger.error("Failed to connect, exiting.", e)
      }
    }
  }

  def connect() = {
    if (!client.isConnected()) {
      isPaused = false
      val th = new Thread(s"slaver-${clientProperties.getMysql.getMyChannel}") {
        override def run(): Unit = getConnected

      }
      th.start()
      true
    }
    else {
      false
    }
  }


  def disconnect() = {
    //    if (client.isConnected()) {
    //      System.out.println(s"myChannel now disconnect ${clientProperties.getMysql.getMyChannel}")
    //      isPaused = true
    client.disconnect()
    //      clusterActorRef ! HeartDestory(clientProperties.getMysql.getMyChannel)
    //      if (!clusterActorRefSchedel.isEmpty)
    //        clusterActorRefSchedel.get.cancel()
    //      true
    //    }
    //    else {
    //      false
    //    }
  }
}
