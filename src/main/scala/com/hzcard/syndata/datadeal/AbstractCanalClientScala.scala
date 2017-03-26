package com.hzcard.syndata.datadeal

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.alibaba.otter.canal.client.CanalConnector
import com.alibaba.otter.canal.protocol.CanalEntry._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.base.Function
import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.hzcard.syndata.SpringExtentionImpl
import com.hzcard.syndata.config.autoconfig.CanalClientContext
import org.slf4j.{LoggerFactory, MDC}
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.ApplicationContext
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by zhangwei on 2017/3/5.
  */
class AbstractCanalClientScala(destination: String, connector: CanalConnector, actorSystem: ActorSystem, context: CanalClientContext, applicationContext: ApplicationContext) extends DisposableBean {
  protected val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
  //  protected val sdf = new SimpleDateFormat(DATE_FORMAT);
  protected var running = false
  implicit val timeout = Timeout(600 seconds)
  val objectMapper = new ObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  val logger = LoggerFactory.getLogger(classOf[AbstractCanalClientScala])
  private val dbActorRefMap = CacheBuilder.newBuilder().build(CacheLoader.from(new Function[String, ActorRef] {
    override def apply(input: String): ActorRef = {
      actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("dbDealDataActor"), input)
    }
  }))

  private val esActorRefMap = CacheBuilder.newBuilder().build(CacheLoader.from(new Function[String, ActorRef] {
    override def apply(input: String): ActorRef = {
      actorSystem.actorOf(SpringExtentionImpl(actorSystem)(applicationContext).props("esDealDataActor"), "es" + input)
    }
  }))

  val schemas = context.getCanalClientProperties.getDestinations.get(destination).getIncludeSchemas.split(",")
  val mapDataSourceLookup = applicationContext.getBean("mapDataSource").asInstanceOf[MapDataSourceLookup]

  val handler = new Thread.UncaughtExceptionHandler() {
    def uncaughtException(t: Thread, e: Throwable) {
      logger.error("parse events has an error", e);
    }
  }

  def start() {
    logger.error("destination start:{} ", destination)
    running = true
    val thread = new Thread(new Runnable() {
      def run {
        acceptData
      }
    });

    thread.setUncaughtExceptionHandler(handler);
    thread.start();

  }

  def stop(): Unit = {
    running = false
    connector.disconnect()
    for (ref <- dbActorRefMap.asMap().values())
      actorSystem.stop(ref)
    for (ref <- esActorRefMap.asMap().values())
      actorSystem.stop(ref)
    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  def acceptData {
    val batchSize = 4 * 1024
    var batchId = 0L
    val toDealDataDatas = CacheBuilder.newBuilder().build(CacheLoader.from(new Function[String, ArrayBuffer[SchemaTableMapData]] {
      override def apply(input: String): ArrayBuffer[SchemaTableMapData] = {
        new ArrayBuffer[SchemaTableMapData]()
      }
    }))
    logger.info("destination {} started ", destination)
    while (running) {
      TimeUnit.MILLISECONDS.sleep(100)
      MDC.put("destination", destination)
      try {
        connector.connect()
        connector.subscribe()

        val message = connector.getWithoutAck(batchSize) // 获取指定数量的数据
        batchId = message.getId
        val size = message.getEntries.size
//        if (logger.isInfoEnabled())
//          logger.info("destination " + destination + " , size is " + size + ", batchId is " + batchId)
        if (batchId != -1 && size > 0) {
          message.getEntries
            .filter(x => (x.getEntryType eq EntryType.ROWDATA) && schemas.contains(x.getHeader.getSchemaName))
            .map(x => SchemaTableRowChange(x.getHeader.getSchemaName, x.getHeader.getTableName, RowChange.parseFrom(x.getStoreValue)))
            .filter(x => !x.rowChange.getEventType.eq(EventType.QUERY) && !x.rowChange.getIsDdl)
            .foreach(x => {
              val key = "(" + destination + ")(" + x.schema + ")(" + x.tableName + ")"
              val rowChangeColumns = getRowChangeColumns(x)
              if (rowChangeColumns.length > 0)
                toDealDataDatas.get(key) ++= rowChangeColumns
            }
            )

          val dealFuture = toDealDataDatas.asMap().map(x => {
            val metaData = getMetaData(x._1)
            //(pre-e-1)(points)(tp_order_point)
            val actorRef = dbActorRefMap.get(actorRefkey(x._1))
            val esActorRef = esActorRefMap.get(actorRefkey(x._1))
            (if (metaData._4) Some(actorRef ? DealDataRequest(mapDataSourceLookup, metaData._1, metaData._2, metaData._3, x._2.toArray)) else None,
              if (metaData._5) Some(esActorRef ? EsDealDataRequest(metaData._6, mapToEntity(x._2, metaData._7))) else None)
          })

          dealFuture.foreach(x => {
            if (!x._1.isEmpty)
              Await.result(x._1.get, timeout.duration).asInstanceOf[Boolean]
            if (!x._2.isEmpty)
              Await.result(x._2.get, timeout.duration).asInstanceOf[Boolean]
          })
          connector.ack(batchId) // 提交确认
//          if (logger.isInfoEnabled())
//            logger.info("destination " + destination + ",batchId {} ack", batchId)
        }

      } catch {
        case e: Throwable => {
          if (batchId != -1)
            connector.rollback(batchId) // 处理失败, 回滚数据,重新处理
          logger.error("repository error!", e)
        }
      } finally {
        toDealDataDatas.invalidateAll()
//        connector.disconnect()
        MDC.remove("destination")
      }
    }

  }

  def actorRefkey(key: String) = {
    val destination = key.split("\\)")(0).substring(1)
    val schema = key.split("\\)")(1).substring(1)
    val tableName = key.split("\\)")(2).substring(1)
    destination + schema + tableName
  }

  def getMetaData(key: String) = {
    var keyWord = "id"
    val schema = key.split("\\)")(1).substring(1)
    val tableName = key.split("\\)")(2).substring(1)
    var isSynTable = false
    val includeSynTablesStr = {
      if (context.getCanalClientProperties.getSchemas.get(schema) != null)
        context.getCanalClientProperties.getSchemas.get(schema).getIncludeSynTables
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
    if (context.getCanalClientProperties.getSchemas.get(schema) != null
      && context.getCanalClientProperties.getSchemas.get(schema).getTableRepository != null
      && context.getCanalClientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      keyWord = if (context.getCanalClientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword == null) "id" else context.getCanalClientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getKeyword
    var repository: String = null
    if (context.getCanalClientProperties.getSchemas.get(schema) != null &&
      context.getCanalClientProperties.getSchemas.get(schema).getTableRepository != null
      && context.getCanalClientProperties.getSchemas.get(schema).getTableRepository.get(tableName) != null)
      repository = context.getCanalClientProperties.getSchemas.get(schema).getTableRepository.get(tableName).getRepository
    var repositoryBean: ElasticsearchRepository[Object, String] = null
    var isSynEss = true
    if (repository == null || repository.trim.length == 0)
      isSynEss = false
    else
      repositoryBean = context.getApplicationContext.getBean(Class.forName(repository)).asInstanceOf[ElasticsearchRepository[Object, String]]
    (schema, tableName, keyWord, isSynTable, isSynEss, repositoryBean, repository)
  }

  def getRowChangeColumns(x: SchemaTableRowChange) = {
    val schemaTableMapDataArr = new ArrayBuffer[SchemaTableMapData]()
    for (rowDatas <- x.rowChange.getRowDatasList) {
      val tempMap = new scala.collection.mutable.LinkedHashMap[String, Object]
      val columnsList = {
        if (x.rowChange.getEventType == EventType.DELETE) {
          rowDatas.getBeforeColumnsList
        } else
          rowDatas.getAfterColumnsList
      }
      for (column <- columnsList)
        if (column.getValue == null || column.getValue == "")
          tempMap.put(column.getName, null)
        else {
          if (column.getMysqlType == "datetime") {
            val format = new SimpleDateFormat(DATE_FORMAT)
            tempMap.put(column.getName, format.parse(column.getValue))
          }
          else
            tempMap.put(column.getName, column.getValue)
        }

      schemaTableMapDataArr += SchemaTableMapData(x.schema, x.tableName, x.rowChange.getEventType, tempMap)
    }
    schemaTableMapDataArr
  }


  @throws[Exception]
  def mapToEntity(maps: ArrayBuffer[SchemaTableMapData], repository: String): Array[SchemaTableMapData] = {
    if ("com.hzcard.syndata.points.repositories.OperationOrderRepository" == repository || "com.hzcard.syndata.points.repositories.OrderRepository" == repository) {
      for (sigle <- maps) {
        sigle.rowChange.put("code_search", sigle.rowChange.get("code").get)
        sigle.rowChange.put("card_no_search", sigle.rowChange.get("card_no").get)
      }
    }
    maps.toArray
  }

  override def destroy(): Unit = {
    stop()
  }
}

case class SchemaTableRowChange(schema: String, tableName: String, rowChange: RowChange)

case class SchemaTableMapData(schema: String, tableName: String, eventType: EventType, rowChange: scala.collection.mutable.LinkedHashMap[String, Object])

case class SchemaTableMapBatchData(schema: String, tableName: String, eventType: EventType, rowChanges: ArrayBuffer[scala.collection.mutable.LinkedHashMap[String, Object]])

abstract class ParentRequest(val mapDataSource: MapDataSourceLookup)

case class NotMatchMessage()

