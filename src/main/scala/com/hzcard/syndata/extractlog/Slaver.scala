package com.hzcard.syndata.extractlog

import java.io.IOException

import akka.actor.ActorSystem
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.hzcard.syndata.config.autoconfig.{DestinationProperty, Encryptor}
import com.hzcard.syndata.datadeal.BinLogPosition
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

import scala.concurrent.Future

/**
  * Created by zhangwei on 2017/4/27.
  * 看构造函数里的connect()
  */
class Slaver(clientProperties:DestinationProperty, system: ActorSystem,applicationContext: ApplicationContext, encryptor:Encryptor, redis:RedisCache) {

  val logger = LoggerFactory.getLogger(classOf[Slaver])
  protected implicit val ec = system.dispatcher

  protected var isPaused = false

  protected lazy val client = new BinaryLogClient(
    clientProperties.getMysql.getHost,
    clientProperties.getMysql.getPort,
    clientProperties.getMysql.getUser,
    clientProperties.getMysql.getPassword
  )

  client.setServerId(clientProperties.getMysql.getServerId)

  /** If we lose the connection to the server retry every `changestream.mysql.keepalive` milliseconds. **/
  client.setKeepAliveInterval(clientProperties.getMysql.getKeepalive)

  /** Register the objects that will receive `onEvent` calls and deserialize data **/
  client.registerEventListener(ChangeStreamEventListener(system,applicationContext).setConfig(clientProperties,encryptor))
  client.setEventDeserializer(ChangestreamEventDeserializer)

  /** Register the object that will receive BinaryLogClient connection lifecycle events **/
  client.registerLifecycleListener(ChangeStreamLifecycleListener(clientProperties.getMysql.getMyChannel,redis))

//  connect()


  def disconnect() = {
    if(client.isConnected()) {
      logger.warn(s"myChannel now disconnect ${clientProperties.getMysql.getMyChannel}")
      isPaused = true
      client.disconnect()
      true
    }
    else {
      false
    }
  }

  def reset() = {
    if(!client.isConnected()) {
      client.setBinlogFilename(null) //scalastyle:ignore
      Future { getConnected }
      true
    }
    else {
      false
    }
  }

  protected def getConnected = {
    /** Finally, signal the BinaryLogClient to start processing events **/
    logger.info(s"Starting ExtractBinLog...")
//    redis.regiestRunnerServer(clientProperties.getMysql.getMyChannel)
    val position = redis.getPosition(clientProperties.getMysql.getMyChannel).getOrElse(BinLogPosition(clientProperties.getMysql.getMyChannel,null,0L))
    logger.info(s"Starting ExtractBinLog... binLogFileName is ${position.binLogFileName},binLogPosition is ${position.binLogPosition}")
    client.setBinlogFilename(position.binLogFileName)
    client.setBinlogPosition(position.binLogPosition)

    while(!isPaused && !client.isConnected) {
      try {
        client.connect()
      }
      catch {
        case e: IOException =>
          logger.error(e.getMessage)
          logger.error("Failed to connect to MySQL to stream the binlog, retrying...",e)
          Thread.sleep(5000)
        case e: Exception =>
          logger.error("Failed to connect, exiting.", e)
      }
    }
  }

  def connect() = {
    if(!client.isConnected()) {
      isPaused = false
      Future { getConnected }
      true
    }
    else {
      false
    }
  }
}
