package com.hzcard.syndata.extractlog.actors

import akka.actor.Actor
import com.hzcard.syndata.extractlog.events.MutationWithInfo
import com.typesafe.config.{Config, ConfigFactory}

class StdoutActor(config: Config = ConfigFactory.load().getConfig("changestream")) extends Actor {
  def receive = {
    case MutationWithInfo(mutation, _, _, _,Some(message: String)) =>
      println(message)
      sender() ! akka.actor.Status.Success(message)
    case _ =>
      sender() ! akka.actor.Status.Failure(new Exception("Received invalid message"))
  }
}
