package com.hzcard.syndata.extractlog.actors

import java.text.{DateFormat, SimpleDateFormat}
import java.util.TimeZone
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.config.autoconfig.{Encryptor, MysqlClientProperties}
import com.hzcard.syndata.extractlog.emitter.{MutationData, QueryInfo, Transaction}
import com.hzcard.syndata.extractlog.events.{MutationWithInfo, Update}
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

object JsonFormatterActor {
  val dateFormatter: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  dateFormatter.setTimeZone(TimeZone.getTimeZone("CCT"))
  val log = LoggerFactory.getLogger(getClass)
}

class JsonFormatterActor(
                          getNextHop: ActorRefFactory => ActorRef,
                          config: MysqlClientProperties,
                          encryptorConfig: Encryptor
                        ) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  protected val nextHop = getNextHop(context)
  protected val log = LoggerFactory.getLogger(getClass)

  protected val includeData = config.getIncludeData
  protected val encryptData = if (!includeData) false else config.getEncryptor.booleanValue()

  protected lazy val encryptorActor = context.actorOf(Props(new EncryptorActor(encryptorConfig)), name = "encryptorActor" + config.getMyChannel)
  protected implicit val TIMEOUT = Timeout(encryptorConfig.getTimeOut.longValue() milliseconds)
  protected implicit val ec = context.dispatcher

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  def receive = {
    case message: MutationWithInfo if message.columns.isDefined => {
      if (log.isInfoEnabled)
        log.info(s"Received ${message.mutation} for table ${message.mutation.database}.${message.mutation.tableName}")
      val primaryKeys = message.columns.get.columns.collect({ case col if col.isPrimary => col.name })
      val rowData = getRowData(message)
      val oldRowData = getOldRowData(message)
      rowData.indices.foreach({ idx =>
        val row = rowData(idx)
        val oldRow = oldRowData.map(_ (idx))
        val pkInfo = mutable.LinkedHashMap(primaryKeys.map({
          case k: String => k -> row.getOrElse(k, JsNull)
        }): _*)
        val transaction = (message.transaction.get.lastMutationInTransaction) match {
          case true => Transaction(message.transaction.get.gtid, true, Some(message.transaction.get.rowCount))
          case false => Transaction(message.transaction.get.gtid, false)
        }
        val mutationData = MutationData(message.mutation.toString, message.mutation.sequence + idx, message.mutation.database, message.mutation.tableName,
          QueryInfo(message.mutation.timestamp, message.mutation.sql.getOrElse(""), rowData.length, idx + 1), pkInfo, transaction, row, oldRow.getOrElse(mutable.LinkedHashMap.empty)
        )
        nextHop ! message.copy(mutationData = mutationData, formattedMessage = Some(""))
//        TimeUnit.MILLISECONDS.sleep(10L)
      })
    }
  }

  protected def getRowData(message: MutationWithInfo) = {
    val columns = message.columns.get.columns
    val mutation = message.mutation

    mutation.rows.map(row =>
      mutable.LinkedHashMap(columns.indices.map({
        case idx if mutation.includedColumns.get(idx) =>
          columns(idx).name -> row(idx)
      }): _*)
    )
  }

  protected def getOldRowData(message: MutationWithInfo) = {
    val columns = message.columns.get.columns
    val mutation = message.mutation

    mutation match {
      case update: Update =>
        Some(update.oldRows.map(row =>
          mutable.LinkedHashMap(columns.indices.map({
            case idx if mutation.includedColumns.get(idx) =>
              columns(idx).name -> row(idx)
          }): _*)
        ))
      case _ => None
    }
  }


  protected def getJsonHeader(
                               message: MutationWithInfo,
                               pkInfo: ListMap[String, JsValue],
                               rowData: ListMap[String, JsValue],
                               currentRow: Long,
                               rowsTotal: Long
                             ): ListMap[String, JsValue] = {
    ListMap(
      "mutation" -> JsString(message.mutation.toString),
      "sequence" -> JsNumber(message.mutation.sequence + currentRow),
      "database" -> JsString(message.mutation.database),
      "table" -> JsString(message.mutation.tableName),
      "query" -> JsObject(
        "timestamp" -> JsNumber(message.mutation.timestamp),
        "sql" -> JsString(message.mutation.sql.getOrElse("")),
        "row_count" -> JsNumber(rowsTotal),
        "current_row" -> JsNumber(currentRow + 1)
      ),
      "primary_key" -> JsObject(pkInfo)
    )
  }

  protected def getJsonRowData(rowData: ListMap[String, JsValue]): ListMap[String, JsValue] = includeData.booleanValue() match {
    case true => ListMap("row_data" -> JsObject(rowData))
    case false => ListMap.empty
  }

  protected def updateInfo(oldRowData: Option[ListMap[String, JsValue]]): ListMap[String, JsValue] = includeData.booleanValue() match {
    case true => oldRowData.map({ row => ListMap("old_row_data" -> JsObject(row)) }).getOrElse(ListMap.empty)
    case false => ListMap.empty
  }
}
