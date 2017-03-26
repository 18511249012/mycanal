package com.hzcard.syndata

import akka.actor.{Actor, IndirectActorProducer}
import org.springframework.context.ApplicationContext


class SpringActorProducer(ctx: ApplicationContext, actorBeanName: String) extends IndirectActorProducer {
  

  override def produce: Actor = ctx.getBean(actorBeanName, classOf[Actor])

  override def actorClass: Class[_ <: Actor] ={
    val act = ctx.getType(actorBeanName).asInstanceOf[Class[_ <: Actor]]
    act
  }
}
