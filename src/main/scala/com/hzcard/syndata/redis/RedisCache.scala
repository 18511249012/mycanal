package com.hzcard.syndata.redis

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import com.hzcard.syndata.config.autoconfig.CanalClientInstanceAutoConfig
import com.hzcard.syndata.datadeal.BinLogPosition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.{RedisTemplate, ValueOperations}
import org.springframework.stereotype.Component

/**
  * Created by zhangwei on 2017/5/15.
  */
@Component
class RedisCache(@Autowired val redisTemplate: RedisTemplate[Object, Object]) {

  val logger = LoggerFactory.getLogger(getClass)

  val mutexKey = "MYCHANNELRUNING-"
  val binLogPersisterKey = "BINLOG-"

  def keepAlive(myChannel: String): Boolean = {
    val operation: ValueOperations[Object, Object] = redisTemplate.opsForValue()
    val key = mutexKey + myChannel
    val mutexValue = InetAddress.getLocalHost().getHostAddress() + ":" + CanalClientInstanceAutoConfig.getPort
    if (redisTemplate.hasKey(key) && {
      val value = operation.get(key).asInstanceOf[String]
      value == mutexValue
    }) {
      redisTemplate.expire(key, 5, TimeUnit.MINUTES)
      true
    } else
      false

  }

  def regiestRunnerServer(myChannel: String) = {
    val operation: ValueOperations[Object, Object] = redisTemplate.opsForValue()
    var ifSent = false
    while (!ifSent) { //循环监听
      ifSent = operation.setIfAbsent(mutexKey + myChannel, InetAddress.getLocalHost().getHostAddress() + ":" + CanalClientInstanceAutoConfig.getPort)
      if (!ifSent) {
        //看看是不是自己
        if (keepAlive(myChannel))
          ifSent = true
        else
          TimeUnit.MINUTES.sleep(1)
      } else
        redisTemplate.expire(mutexKey + myChannel, 5, TimeUnit.MINUTES)
    }
  }

  def savePosition(positon: BinLogPosition): Unit = {
    val operation: ValueOperations[Object, Object] = redisTemplate.opsForValue()
    val keyfile = binLogPersisterKey + positon.myChannel
    operation.set(keyfile, positon.binLogFileName + "|" + positon.binLogPosition)
  }

  def getPosition(myChannel: String): Option[BinLogPosition] = {
    val operation: ValueOperations[Object, Object] = redisTemplate.opsForValue()
    val positon = operation.get(binLogPersisterKey + myChannel)
    if (positon != null) {
      val fp = positon.asInstanceOf[String].split("\\|")
      Some(BinLogPosition(myChannel, fp(0), java.lang.Long.parseLong(fp(1))))
    } else
      None
  }

  def unRegiestSelfRunnerServer(myChannel: String) = {
    val mutexValue = InetAddress.getLocalHost().getHostAddress() + ":" + CanalClientInstanceAutoConfig.getPort
    val operation: ValueOperations[Object, Object] = redisTemplate.opsForValue()
    val key = mutexKey + myChannel
    if (redisTemplate.hasKey(key) && {
      val value = operation.get(key).asInstanceOf[String]
      value == mutexValue
    }) //删除自身注册的存活
      redisTemplate.delete(mutexKey + myChannel)
  }
}
