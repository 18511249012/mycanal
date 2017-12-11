package com.hzcard.syndata.lister

import akka.actor.{Actor, ActorLogging, DeadLetter}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.extractlog.events.MutationWithInfo

class DeadLettersActor extends Actor with ActorLogging with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  def receive = {
    case d: DeadLetter => {
      if (log.isWarningEnabled) {
        val event = {
          if (d.message.isInstanceOf[MutationWithInfo])
            if (d.message.asInstanceOf[MutationWithInfo].mutation != null) d.message.asInstanceOf[MutationWithInfo].mutation.toString
            else ""
          else
            ""
        }
        log.warning(s"dead Letters retrying send , data event is ${event} , sender is ${d.sender},recipient is ${d.recipient}, data ${objectMapper.writeValueAsString(d.message)}")
      }
      if (d.recipient.path.name.indexOf("DeadLettersActor") < 0)
        d.recipient ! d.message
    }
  }
}

