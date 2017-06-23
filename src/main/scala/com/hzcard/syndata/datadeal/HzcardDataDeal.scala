package com.hzcard.syndata.datadeal

import akka.actor.Actor
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.pattern.ask
import akka.util.Timeout
import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.SpringExtentionImpl
import com.hzcard.syndata.config.autoconfig.CanalClientProperties
import com.hzcard.syndata.extractlog.emitter.SourceDataSourceChangeEvent
import com.hzcard.syndata.redis.RedisCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Lazy, Scope}
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import org.springframework.stereotype.Component

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/5/12.
  */
@Component("hzcardDataDealActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy(false)
class HzcardDataDealActor(@Autowired val clientProperties: CanalClientProperties, @Autowired val applicationContext: ApplicationContext, @Autowired val redis: RedisCache) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {
  val log = LoggerFactory.getLogger(getClass)
  implicit val timeout = Timeout(60 minutes)

  val actorRef = context.actorOf(SpringExtentionImpl(context.system)(applicationContext).props("dbDealDataActor"), "dbDealDataActor")
  val esActorRef = context.actorOf(SpringExtentionImpl(context.system)(applicationContext).props("esDealDataActor"), "esDealDataActor")
  val mapDataSourceLookup = applicationContext.getBean("mapDataSource").asInstanceOf[MapDataSourceLookup]

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  override def receive: Receive = {
    case x: Array[SourceDataSourceChangeEvent] => try {
      val start = System.currentTimeMillis()
      val myChannelMap = mutable.HashMap[String, Tuple2[String, Long]]()
      val datas = x.map(d => {
        if (myChannelMap.contains(d.myChannel))
          myChannelMap.remove(d.myChannel)
        if (!d.binLogFileName.isEmpty && !d.binLogPosition.isEmpty)
          myChannelMap.put(d.myChannel, (d.binLogFileName.get, d.binLogPosition.get))
        d.event.map(e => {
          if (log.isDebugEnabled()) {
            log.debug(s"HzcardDataDealActor mutation is ${e.mutation}, old_row_data is ${objectMapper.writeValueAsString(e.old_row_data)}, row_data is ${e.row_data}")
          }
          SchemaTableMapData(e.database, e.table, EventType.valueOf(e.mutation.toUpperCase), e.row_data, e.transaction.id)
        })
      }
      ).reduce(_ ++ _) //转为SchemaTableMapData
        .groupBy(d => (d.schema, d.tableName, d.transId)) //根据schema\tableName\txId分组
      val dealFuture = datas.map(z => {
        val metaData = getMetaData(z._1._1, z._1._2)
        (if (metaData._4) Some(actorRef ? DealDataRequest(mapDataSourceLookup, metaData._1, metaData._2, metaData._3, z._2.toArray)) else None,
          if (metaData._5) Some(esActorRef ? EsDealDataRequest(metaData._6, mapToEntity(z._2, metaData._7))) else None)
      })
      dealFuture.foreach(f => {
        if (!f._1.isEmpty)
          Await.result(f._1.get, timeout.duration).asInstanceOf[Boolean]
        if (!f._2.isEmpty)
          Await.result(f._2.get, timeout.duration).asInstanceOf[Boolean]
      })
      myChannelMap.foreach(bPosition => {
        log.info(s"myChannel= ${bPosition._1},binlogFileName = ${bPosition._2._1},bPosition = ${bPosition._2._2}")
        redis.savePosition(BinLogPosition(bPosition._1, bPosition._2._1, bPosition._2._2))
      })
      sender ! true
    } catch {
      case th: Throwable =>
        log.info(s"data is ${objectMapper.writeValueAsString(x)}")
        log.error(s"处理数据失败：" + th.getMessage, th)
        sender() ! akka.actor.Status.Failure(th)
    }
    case _ =>
      log.error(s"Received invalid message.")
      sender() ! akka.actor.Status.Failure(new Exception("Received invalid message"))
  }

  def getMetaData(schema: String, tableName: String) = {
    var keyWord = "id"
    var isSynTable = false
    val includeSynTablesStr = {
      if (clientProperties.getSchemas.get(schema) != null)
        clientProperties.getSchemas.get(schema).getIncludeSynTables
      else null
    }
    val includeSynTables: Array[String] = {
      if (includeSynTablesStr != null)
        includeSynTablesStr.split(",")
      else
        null
    }
    if (includeSynTables == null || includeSynTables.length == 0)
      isSynTable = false
    else
      for (includeSynTable <- includeSynTables) {
        if (includeSynTable.equalsIgnoreCase(tableName))
          isSynTable = true
      }
    if (clientProperties.getSchemas.get(schema) != null
      && clientProperties.getSchemas.get(schema).getTableRepository != null
      && clientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      keyWord = if (clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword == null) "id" else clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword
    var repository: String = null
    if (clientProperties.getSchemas.get(schema) != null &&
      clientProperties.getSchemas.get(schema).getTableRepository != null
      && clientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      repository = clientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getRepository
    var repositoryBean: ElasticsearchRepository[Object, String] = null
    var isSynEss = true
    if (repository == null || repository.trim.length == 0)
      isSynEss = false
    else
      repositoryBean = applicationContext.getBean(Class.forName(repository)).asInstanceOf[ElasticsearchRepository[Object, String]]
    (schema, tableName, keyWord, isSynTable, isSynEss, repositoryBean, repository)
  }

  @throws[Exception]
  def mapToEntity(maps: Array[SchemaTableMapData], repository: String): Array[SchemaTableMapData] = {
    if ("com.hzcard.syndata.points.repositories.OperationOrderRepository" == repository || "com.hzcard.syndata.points.repositories.OrderRepository" == repository) {
      for (sigle <- maps) {
        sigle.rowChange.put("code_search", sigle.rowChange.get("code").get)
        sigle.rowChange.put("card_no_search", sigle.rowChange.get("card_no").get)
      }
    }
    maps.toArray
  }
}

case class BinLogPosition(myChannel: String, binLogFileName: String, binLogPosition: Long)

