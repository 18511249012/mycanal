package com.hzcard.syndata

import java.util.concurrent.TimeUnit

import org.apache.catalina.connector.Connector
import org.apache.tomcat.util.threads.ThreadPoolExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent

/**
  * Created by zhangwei on 2017/7/18.
  */
class GracefulShutdown(val shutdownTimeout: Long, val unit: TimeUnit) extends TomcatConnectorCustomizer with ApplicationListener[ContextClosedEvent] {

  @volatile var connector: Connector = null

  val logger = LoggerFactory.getLogger(getClass)

  override def customize(connector: Connector): Unit = this.connector = connector

  override def onApplicationEvent(event: ContextClosedEvent): Unit = if (this.connector != null) awaitTermination(this.connector)

  def awaitTermination(connector: Connector): Unit = {
    connector.pause()
    val executor = connector.getProtocolHandler.getExecutor
    if (executor.isInstanceOf[ThreadPoolExecutor]) {
      try {
        logger.warn(s"Context closed. Going to await termination for ${shutdownTimeout} ${unit} ")
        executor.asInstanceOf[ThreadPoolExecutor].shutdown()
        if (executor.asInstanceOf[ThreadPoolExecutor].awaitTermination(shutdownTimeout, unit)) {
          logger.warn(s"Tomcat thread pool did not shut down gracefully within ${shutdownTimeout} ${unit}. Proceeding with forceful shutdown")
        }
      } catch {
        case x: Throwable => Thread.currentThread().interrupt()
      }
    }else
      logger.warn(s"executor not is InstanceOf[ThreadPoolExecutor], excutor is ${executor.getClass}")
  }
}
