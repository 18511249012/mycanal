package com.hzcard.syndata.logger

import akka.actor.Actor
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.event.LoggerMessageQueueSemantics
import akka.event.Logging.{Debug, Error, Info, InitializeLogger, LogEvent, LogEventWithMarker, LoggerInitialized, Warning}
import akka.event.slf4j.{Logger, SLF4JLogging, Slf4jLogMarker}
import akka.util.Helpers
import org.slf4j.{MDC, Marker, MarkerFactory}

/**
  * SLF4J logger.
  *
  * The thread in which the logging was performed is captured in
  * Mapped Diagnostic Context (MDC) with attribute name "sourceThread".
  */
class CustomSlf4jLogger extends Actor with SLF4JLogging with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val mdcThreadAttributeName = "sourceThread"
  val mdcActorSystemAttributeName = "sourceActorSystem"
  val mdcAkkaSourceAttributeName = "akkaSource"
  val mdcAkkaTimestamp = "akkaTimestamp"

  def receive = {

    case event @ Error(cause, logSource, logClass, message) ⇒
      withMdc(logSource, event) {
        cause match {
          case Error.NoCause | null ⇒
            Logger(logClass, logSource).error(markerIfPresent(event), if (message != null) message.toString else null)
          case _ ⇒
            Logger(logClass, logSource).error(markerIfPresent(event), if (message != null) message.toString else cause.getLocalizedMessage, cause)
        }
      }

    case event @ Warning(logSource, logClass, message) ⇒
      withMdc(logSource, event) {
        Logger(logClass, logSource).warn(markerIfPresent(event), "{}", message.asInstanceOf[AnyRef])
      }

    case event @ Info(logSource, logClass, message) ⇒
      withMdc(logSource, event) {
        Logger(logClass, logSource).info(markerIfPresent(event), "{}", message.asInstanceOf[AnyRef])
      }

    case event @ Debug(logSource, logClass, message) ⇒
      withMdc(logSource, event) {
        Logger(logClass, logSource).debug(markerIfPresent(event), "{}", message.asInstanceOf[AnyRef])
      }

    case InitializeLogger(_) ⇒
      log.info("Slf4jLogger started")
      sender() ! LoggerInitialized
  }

  @inline
  final def withMdc(logSource: String, logEvent: LogEvent)(logStatement: ⇒ Unit) {
    MDC.put(mdcAkkaSourceAttributeName, logSource)
    MDC.put(mdcThreadAttributeName, logEvent.thread.getName)
    MDC.put(mdcAkkaTimestamp, formatTimestamp(logEvent.timestamp))
    MDC.put(mdcActorSystemAttributeName, actorSystemName)
    logEvent.mdc foreach { case (k, v) ⇒ MDC.put(k, String.valueOf(v)) }
    try logStatement finally {
      MDC.remove(mdcAkkaSourceAttributeName)
      MDC.remove(mdcThreadAttributeName)
      MDC.remove(mdcAkkaTimestamp)
      MDC.remove(mdcActorSystemAttributeName)
      logEvent.mdc.keys.foreach(k ⇒ MDC.remove(k))
    }
  }

  private final def markerIfPresent(event: LogEvent): Marker =
    event match {
      case m: LogEventWithMarker ⇒
        m.marker match {
          case slf4jMarker: Slf4jLogMarker ⇒ slf4jMarker.marker
          case marker                      ⇒ MarkerFactory.getMarker(marker.name)
        }
      case _ ⇒ null
    }

  /**
    * Override this method to provide a differently formatted timestamp
    * @param timestamp a "currentTimeMillis"-obtained timestamp
    * @return the given timestamp as a UTC String
    */
  protected def formatTimestamp(timestamp: Long): String =
    Helpers.currentTimeMillisToUTCString(timestamp)

  private val actorSystemName = context.system.name
}
