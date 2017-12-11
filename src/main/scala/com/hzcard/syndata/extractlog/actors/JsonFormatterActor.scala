package com.hzcard.syndata.extractlog.actors

import java.io
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorRef, OneForOneStrategy}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.hzcard.syndata.extractlog.emitter.{MutationData, QueryInfo, Transaction}
import com.hzcard.syndata.extractlog.events.{Column, MutationWithInfo, Update}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import spray.json._

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.language.postfixOps

object JsonFormatterActor {
  val log = LoggerFactory.getLogger(getClass)
}

@Component("jsonFormatterActor")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class JsonFormatterActor(
                          @Autowired @Qualifier("emitterLoaderRef") nextHop: ActorRef
                        ) extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  val log = LoggerFactory.getLogger(getClass)

  protected val includeData = true

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)


  override def preStart(): Unit = {
    log.info("JsonFormatterActor is start")
    super.preStart()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason.getMessage, reason)
    self ! message
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Throwable => Restart
  }

  def receive = {
    case message: MutationWithInfo if message.columns.isDefined => {
      if (log.isDebugEnabled)
        log.debug(s"Received ${message.mutation} for table ${message.mutation.database}.${message.mutation.tableName}")
      val primaryKeys = message.columns.get.columns.collect({ case col if col.isPrimary => col.name })
      val rowData = getRowData(message)
      val oldRowData = getOldRowData(message)
      rowData.indices.foreach({ idx =>
        val row = rowData(idx)
        val oldRow = oldRowData.map(_ (idx))
        val pkInfo = mutable.LinkedHashMap(primaryKeys.map({
          case k: String => k -> row.getOrElse(k, JsNull)
        }): _*)
        val transaction = (idx == rowData.length - 1) match {
          case true => Transaction(message.transaction.get.gtid, true, Some(message.transaction.get.rowCount))
          case false => Transaction(message.transaction.get.gtid, false)
        }
        val mutationData = MutationData(message.mutation.toString,
          message.mutation.sequence + idx, message.mutation.database, message.mutation.tableName,
          QueryInfo(message.mutation.timestamp, message.mutation.sql.getOrElse(""), rowData.length, idx + 1), pkInfo, transaction, row, oldRow.getOrElse(mutable.LinkedHashMap.empty)
          , getRowDataType(message.columns.get.columns)
        )
        val newMessage = message.copy(mutationData = mutationData, formattedMessage = Some(""))
        message == null
        if (log.isInfoEnabled)
          log.info(objectMapper.writeValueAsString(newMessage))
        nextHop ! newMessage
      })
    }
  }

  //日期偏移处理
  def correctTime(serializable: io.Serializable): Long = {
    val ca = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    ca.setTime(serializable.asInstanceOf[java.util.Date])
    ca.add(Calendar.MILLISECOND, ca.get(Calendar.ZONE_OFFSET) - TimeZone.getTimeZone("GMT+8").getRawOffset)
    ca.getTime.getTime
  }

  protected def getRowData(message: MutationWithInfo) = {
    val columns = message.columns.get.columns
    val mutation = message.mutation

    mutation.rows.map(row =>
      mutable.LinkedHashMap(columns.indices.map({
        case idx if mutation.includedColumns.get(idx) =>
          if (row(idx) != null && row(idx).isInstanceOf[java.util.Date]) {
            columns(idx).name -> new Timestamp(correctTime(row(idx)))
          } else
            columns(idx).name -> row(idx)

        case idx if !mutation.includedColumns.get(idx) => log.error(s"columns ${columns(idx).name}"); columns(idx).name -> null
      }): _*)
    )
  }

  protected def getRowDataType(columns: IndexedSeq[Column]) = {
    columns.foldLeft(mutable.LinkedHashMap.empty[String, String])((map, col) => {
      map.put(col.name, col.dataType);
      map
    })
  }


  protected def getOldRowData(message: MutationWithInfo) = {
    val columns = message.columns.get.columns
    val mutation = message.mutation

    mutation match {
      case update: Update =>
        Some(update.oldRows.map(row =>
          mutable.LinkedHashMap(columns.indices.map({
            case idx if mutation.includedColumns.get(idx) =>
              if (row(idx) != null && row(idx).isInstanceOf[java.util.Date])
                columns(idx).name -> new Timestamp(correctTime(row(idx)))
              else {
                columns(idx).name -> row(idx)
              }
            case idx if !mutation.includedColumns.get(idx) => log.error(s"columns ${columns(idx).name}"); columns(idx).name -> null
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
