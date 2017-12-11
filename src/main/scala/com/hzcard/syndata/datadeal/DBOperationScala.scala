package com.hzcard.syndata.datadeal

import java.io.{ByteArrayInputStream, InputStreamReader, StringReader}
import java.util.concurrent.ArrayBlockingQueue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
import java.nio.charset.Charset
import java.sql.{Connection, PreparedStatement, SQLException}

import scala.collection.mutable

object DBOperationScala {

  val instance = new DBOperationScala()

  def apply(sources: MapDataSourceLookup): DBOperationScala = { //改成单例
    if (instance.sources == null)
      instance.sources = sources
    instance
  }

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

}

class DBOperationScala(var sources: MapDataSourceLookup) {


  val logger: Logger = LoggerFactory.getLogger(classOf[DBOperationScala])

  def this() = this(null)


  def delete(schema: String, tableName: String, keyword: String, maps: Array[mutable.LinkedHashMap[String, java.io.Serializable]]): Boolean = {
    var conn: Connection = null
    var preparedStatement: PreparedStatement = null
    conn = sources.getDataSource("target" + schema).getConnection
    conn.setAutoCommit(false)
    val isOracle = conn.getMetaData.getDriverName.indexOf("Oracle") >= 0
    val sql: StringBuilder = new StringBuilder("delete from " + schema + ".")
    if (isOracle)
      sql.append("\"" + tableName + "\"")
    else
      sql.append(tableName)
    try {
      val keywords: Array[String] = if (keyword == null) null else keyword.split(",")


      sql.append(" where ")
      if (keywords == null || keywords.length == 0 || (keywords.length == 1 && keywords(0) == "id")) {
        if (isOracle)
          sql.append("\"id\"=?")
        else
          sql.append("  id = ? ")
      }
      else {

        for (key <- keywords) {
          if (isOracle)
            sql.append(" \"" + key.toLowerCase + "\"=? and")
          else
            sql.append(" " + key + "=? and")
        }
        sql.delete(sql.length - 3, sql.length)
      }
      //      if (logger.isInfoEnabled) logger.info("delete sql is: {}",sql.toString())
      if (logger.isDebugEnabled) {
        logger.debug("delete sql is: {}", sql.toString())
        logger.debug(s"delete datamap is: ${DBOperationScala.objectMapper.writeValueAsString(maps)}")
      }
      preparedStatement = conn.prepareStatement(sql.toString)
      //      logger.error(" table {} delete batch size is {}",tableName,maps.size)
      val startTime = System.currentTimeMillis()
      for (map <- maps) {
        var i: Int = 1
        if (keywords == null || keywords.length == 0 || (keywords.length == 1 && keywords(0) == "id")) {
          preparedStatement.setObject(i, map.get("id").get)
          i += 1
        }
        else {
          for (key <- keywords) {
            preparedStatement.setObject(i, map.get(key).get)
            i += 1
          }
        }
        preparedStatement.addBatch()
      }
      preparedStatement.executeBatch()
      conn.commit()
      //      logger.error(" table {} delete batch end time {}",tableName,(System.currentTimeMillis()-startTime))
      true
    }
    catch {
      case e: SQLException => {
        conn.rollback()
        logger.error(s"delete execute sql is ${sql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(maps)}", e)
        throw e
      }
      case e: Throwable => {
        conn.rollback()
        logger.error(s"delete execute sql is ${sql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(maps)}", e)
        throw e
      }
    } finally {
      try {
        conn.close()
        preparedStatement.close()
      }
      catch {
        case e: SQLException => {
          logger.error(e.getMessage)
        }
      }
    }
  }

  @throws[Exception]
  def save(schema: String, tableName: String, keyword: String, map: Array[mutable.LinkedHashMap[String, java.io.Serializable]], txId: String, columnTypes: collection.mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty[String, String]): Boolean = {
    if (map.size == 0) {
      logger.warn(s"save table ${schema}.${tableName},datasize is ${map.size}")
      true
    } else {

      var conn: Connection = null
      var preparedStatement: PreparedStatement = null
      conn = sources.getDataSource("target" + schema).getConnection
      conn.setAutoCommit(false)
      val isOracle = conn.getMetaData.getDriverName.indexOf("Oracle") >= 0

      val strSql: StringBuilder = new StringBuilder("insert into " + schema + ".")
      if (isOracle)
        strSql.append("\"" + tableName + "\"(")
      else
        strSql.append(tableName + "(")
      try {
        for (key <- map(0).keySet) {
          if (isOracle)
            strSql.append("\"" + key.toLowerCase + "\",")
          else
            strSql.append(key + ",")
        }
        strSql.deleteCharAt(strSql.length - 1)
        strSql.append(") values(")

        for (i <- 0 until map(0).size) {
          strSql.append("?,")
        }
        strSql.deleteCharAt(strSql.length - 1)
        strSql.append(")")
        preparedStatement = conn.prepareStatement(strSql.toString)
        var startTime = System.currentTimeMillis()
        for (objMap <- map) {
          var i: Int = 1
          for (key <- objMap.keySet) {
            if (isOracle && columnTypes.get(key).getOrElse("") == "text" && objMap.get(key).get != null) {
//              logger.error(s"text type is ${objMap.get(key).get.getClass}")
              val clobStr= new String(objMap.get(key).get.asInstanceOf[Array[Byte]],Charset.forName("utf-8"))
              preparedStatement.setCharacterStream(i, new StringReader(clobStr), clobStr.length)
            }else
              preparedStatement.setObject(i, objMap.get(key).get)
            i += 1
          }
          preparedStatement.addBatch()
        }
        startTime = System.currentTimeMillis()
        preparedStatement.executeBatch()
        conn.commit();
        true
      }
      catch {
        case e: Throwable =>
          conn.rollback();
          if (e.getMessage.indexOf("Duplicate entry") >= 0 || e.getMessage.indexOf("ORA-00001") >= 0) {
            logger.warn(s"save table duplicate entry ${schema}.${tableName},datasize is ${map.size}，txId is ${txId}", e)
            true
          } else {

            if (e.getMessage.indexOf("Deadlock") < 0) //因为同步目标方的死锁频繁，这里不打印
              logger.error(s"insert execute sql is ${strSql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}", e)
            throw e
          }


      } finally {
        preparedStatement.close()
        conn.close()
      }
    }
  }


  def update(schema: String, tableName: String, keyword: String, map: Array[mutable.LinkedHashMap[String, java.io.Serializable]], columnTypes: collection.mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty[String, String]): Boolean = {
    var conn: Connection = null
    var preparedStatement: PreparedStatement = null
    conn = sources.getDataSource("target" + schema).getConnection
    conn.setAutoCommit(false)
    val isOracle: Boolean = conn.getMetaData.getDriverName.indexOf("Oracle") >= 0
    val strSql = new StringBuilder("update " + schema + ".")
    if (isOracle)
      strSql.append("\"" + tableName + "\" set ")
    else
      strSql.append(tableName + " set ")
    try {


      for (key <- map(0).keySet) {
        if (!keyword.split(",").contains(key)) {
          if (isOracle)
            strSql.append("\"" + key.toLowerCase + "\" = ?,")
          else
            strSql.append(key + " = ?,")
        }
      }
      strSql
        .deleteCharAt(strSql.toString.trim.length - 1)
      strSql.append(" where ")

      for (key <- keyword.split(",")) {
        if (isOracle)
          strSql.append(" \"" + key.toLowerCase() + "\"=? and")
        else
          strSql.append(" " + key + "=? and")
      }
      strSql.delete(strSql.length - 3, strSql.length)

      if (logger.isDebugEnabled) {
        logger.debug("updateSingle sql is: {}", strSql.toString())
        logger.debug(s"update datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}")
      }
      preparedStatement = conn.prepareStatement(strSql.toString)
      for (objMap <- map) {
        var i = 1
        for (objKey <- objMap.keySet) {
          if (!keyword.split(",").contains(objKey)) {
            val value = objMap.get(objKey)
            if (isOracle && columnTypes.get(objKey).getOrElse("") == "text" && value.get != null) {
              preparedStatement.setCharacterStream(i, new InputStreamReader(new ByteArrayInputStream(value.get.asInstanceOf[Array[Byte]]),Charset.forName("utf-8")), value.get.asInstanceOf[Array[Byte]].length)
            }else
              preparedStatement.setObject(i, value.get)
            i += 1
          }
        }
        for (key <- keyword.split(",")) {
          val value = objMap.get(key)
          preparedStatement.setObject(i, value.get)
          i += 1
        }
        preparedStatement.addBatch()
      }
      preparedStatement
        .executeBatch()
      conn.commit()
      //      logger.error(" table {} update batch end time {}",tableName,(System.currentTimeMillis()-startTime))
      true
    } catch {
      case e: Throwable => {
        conn.rollback()
        logger.error(s"update execute sql is ${strSql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}", e)
        throw e
      }
    } finally {
      if (preparedStatement != null) preparedStatement.close()
      if (conn != null) conn.close()
    }
  }

}