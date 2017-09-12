package com.hzcard.syndata.extractlog.actors

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, OneForOneStrategy}
import com.hzcard.syndata.extractlog.events.{MutationEvent, MutationWithInfo}

import scala.language.postfixOps
//import com.amazonaws.services.sns.AmazonSNSAsyncClient
//import com.amazonaws.services.sns.model.CreateTopicResult
//import com.github.dwhjames.awswrap.sns.AmazonSNSScalaClient
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

object SnsActor {
  def getTopic(mutation: MutationEvent, topic: String, topicHasVariable: Boolean = true): String = {
    val database = mutation.database.replaceAll("[^a-zA-Z0-9\\-_]", "-")
    val tableName = mutation.tableName.replaceAll("[^a-zA-Z0-9\\-_]", "-")
    topicHasVariable match {
      case true => topic.replace("{database}", database).replace("{tableName}", tableName)
      case false => topic
    }
  }
}

class SnsActor(config: Config = ConfigFactory.load().getConfig("changestream")) extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(loggingEnabled = true) {
      case _: Exception => Escalate
    }

  protected val log = LoggerFactory.getLogger(getClass)
  protected implicit val ec = context.dispatcher

  protected val TIMEOUT = config.getLong("aws.timeout")

  protected val snsTopic = config.getString("aws.sns.topic")
  protected val snsTopicHasVariable = snsTopic.contains("{")

//  protected val client = new AmazonSNSScalaClient(new AmazonSNSAsyncClient())
//  protected val topicArns = mutable.HashMap.empty[String, Future[CreateTopicResult]]

  def receive = {
    case MutationWithInfo(mutation, _, _, _, Some(message: String)) =>
      log.debug(s"Received message: ${message}")
      send(mutation, message)
    case _ =>
      log.error(s"Received invalid message.")
      sender() ! akka.actor.Status.Failure(new Exception("Received invalid message"))
  }

  protected def getTopic(mutation: MutationEvent): String = {
    SnsActor.getTopic(mutation, snsTopic, snsTopicHasVariable)
  }

  protected def send(mutation: MutationEvent, message: String) = {
    val origSender = sender()
//    val topic = getTopic(mutation)
//    val topicArn = topicArns.getOrElse(topic, client.createTopic(topic))
//    topicArns.update(topic, topicArn)
//
//    val request = topicArn.flatMap(topic => client.publish(topic.getTopicArn, message))
//
//    request onComplete {
//      case Success(result) =>
//        log.debug(s"Successfully published message to ${snsTopic} (messageId ${result.getMessageId})")
    origSender ! akka.actor.Status.Success(message)
//      case Failure(exception) =>
//        log.error(s"Failed to publish to topic ${snsTopic}.", exception)
//        origSender ! akka.actor.Status.Failure(exception)
//    }
  }
}
