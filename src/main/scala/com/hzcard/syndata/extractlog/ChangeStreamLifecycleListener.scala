package com.hzcard.syndata.extractlog

import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.BinaryLogClient.LifecycleListener
import com.hzcard.syndata.extractlog.ChangeStreamLifecycleListener.log
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory

object ChangeStreamLifecycleListener {
  protected val log = LoggerFactory.getLogger(getClass)

  def apply(myChannel: String, redis: RedisCache): ChangeStreamLifecycleListener = new ChangeStreamLifecycleListener(myChannel, redis)
}

class ChangeStreamLifecycleListener(myChannel: String, redis: RedisCache) extends LifecycleListener {

//  var runFlag = false

  def onConnect(client: BinaryLogClient) = {
    log.warn(s"MySQL client connected!")
//    runFlag = true
//    //启动监听线程
//    val redisKeepAlive = new Thread("redis-keep-alive" + myChannel) {
//      override def run(): Unit = {
//        while (runFlag) {
//          if (redis.keepAlive(myChannel))
//            TimeUnit.SECONDS.sleep(5L)
//          else
//            runFlag = false;
//        }
//      }
//    }
//    redisKeepAlive.start()
  }

  def onDisconnect(client: BinaryLogClient) = {
    log.warn(s"MySQL binlog client was disconnected. ${myChannel}")
//    runFlag = false
    redis.unRegiestSelfRunnerServer(myChannel)
  }

  def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception) = {
    log.error(s"MySQL client failed to deserialize event:\n${ex}\n\n")
    throw new Exception(s"MySQL client failed to deserialize event.", ex)
  }

  def onCommunicationFailure(client: BinaryLogClient, ex: Exception) = {
    log.error(s"MySQL client communication failure:\n${ex}\n\n", ex)

    if (ex.isInstanceOf[com.github.shyiko.mysql.binlog.network.ServerException]) {
      // If the server immediately replies with an exception, sleep for a bit
      Thread.sleep(5000)
    }
  }

}
