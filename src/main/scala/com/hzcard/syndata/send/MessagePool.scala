package com.hzcard.syndata.send

import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap, TimeUnit}
import java.util.function.BiFunction
import javax.annotation.PreDestroy

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.hzcard.syndata.datadeal.{BinLogPosition, DbDealDataActor, EsDealDataActor, HzcardDataDealActor}
import com.hzcard.syndata.extractlog.actors.{ColumnInfoActor, JsonFormatterActor, TransactionActor}
import com.hzcard.syndata.extractlog.emitter.{EmitterActor, SourceDataSourceChangeEvent}
import com.hzcard.syndata.persist._
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.concurrent.Await
import scala.concurrent.duration._

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
class MessagePool(@Autowired system: ActorSystem, @Autowired applicationContext: ApplicationContext) {

  implicit val timeout = Timeout(60 seconds)
  val log = LoggerFactory.getLogger(getClass)

  val cacheMTId = new ArrayBlockingQueue[String](500) //最多缓存
  val cacheMessage = new ConcurrentHashMap[String, SourceDataSourceChangeEvent]
  var isSend = true

  protected lazy val persistorLoader: (ActorRefFactory => ActorRef) = (_ => system.actorOf(Props(new PositionPersistActor(applicationContext)), "persistorLoader"))

  protected lazy val esDealDataActor = system.actorOf(Props(new EsDealDataActor(persistorLoader, applicationContext)), "esDealDataActor")
  protected lazy val dbDealDataActor = system.actorOf(Props(new DbDealDataActor(_ => esDealDataActor, applicationContext)), "dbDealDataActor")
  protected lazy val hzcardDataDealActor = system.actorOf(Props(new HzcardDataDealActor(_ => dbDealDataActor)), "hzcardDataDealActor")


  val sendMessage = new Thread("emitter-send-thread") {
    override def run(): Unit = {
      while (isSend) {
        val txId = cacheMTId.take()
        val singleTrans = cacheMessage.get(txId)
        if (singleTrans != null) {
          hzcardDataDealActor ! singleTrans
          cacheMessage.remove(txId)
        }
      }
    }
  }
  sendMessage.start()

  def compute(id: String, adderSupplier: BiFunction[String, SourceDataSourceChangeEvent, SourceDataSourceChangeEvent]) = cacheMessage.compute(id, adderSupplier)

  def putTx(id: String) = this.cacheMTId.put(id)

  @PreDestroy
  def destory(): Unit = {
    isSend = false
    TimeUnit.SECONDS.sleep(5L)
  }

}
