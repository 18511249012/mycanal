package com.hzcard.syndata.datadeal

import java.sql.{Connection, PreparedStatement, SQLException}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup

import scala.collection.mutable

object DBOperationScala {
  def apply(sources: MapDataSourceLookup): DBOperationScala = new DBOperationScala(sources)
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
}

class DBOperationScala(val sources: MapDataSourceLookup) {


  val logger: Logger = LoggerFactory.getLogger(classOf[DBOperationScala])


  def delete(schema: String, tableName: String, keyword: String, maps: Array[mutable.LinkedHashMap[String, java.io.Serializable]]): Boolean = {
    var conn: Connection = null
    var preparedStatement: PreparedStatement = null
    val sql: StringBuilder = new StringBuilder("delete from " + schema + "." + tableName)
    try {
      val keywords: Array[String] = if (keyword == null) null else keyword.split(",")
      conn = sources.getDataSource("target" + schema).getConnection

      sql.append(" where ")
      if (keywords == null || keywords.length == 0 || (keywords.length == 1 && keywords(0) == "id")) {
        sql.append("  id = ? ")
      }
      else {

        for (key <- keywords)
          sql.append(" " + key + "=? and")
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
      //      logger.error(" table {} delete batch end time {}",tableName,(System.currentTimeMillis()-startTime))
      true
    }
    catch {
      case e: SQLException => {
        logger.error(s"delete execute sql is ${sql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(maps)}",e)
        throw e
      }
      case e:Throwable =>{
        logger.error(s"delete execute sql is ${sql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(maps)}",e)
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
  def save(schema: String, tableName: String, keyword: String, map: Array[mutable.LinkedHashMap[String, java.io.Serializable]]): Boolean = {
    var conn: Connection = null
    var preparedStatement: PreparedStatement = null
    val strSql: StringBuilder = new StringBuilder("insert into " + schema + "." + tableName + "(")
    try {
      conn = sources.getDataSource("target" + schema).getConnection

      for (key <- map(0).keySet) {
        strSql.append(key + ",")
      }
      strSql.deleteCharAt(strSql.length - 1)
      strSql.append(") values(")

      for (i <- 0 until map(0).size) {
        strSql.append("?,")
      }
      strSql.deleteCharAt(strSql.length - 1)
      strSql.append(")")

      if (logger.isDebugEnabled) {
        logger.debug("insert sql is: {}", strSql.toString())
        logger.debug(s"insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}")
      }
      //      if (logger.isInfoEnabled) logger.info("insert sql is: {}" , strSql.toString)
      preparedStatement = conn.prepareStatement(strSql.toString)
      //      logger.error(" table {} insert batch size is {}",tableName,map.size)
      val startTime = System.currentTimeMillis()
      for (objMap <- map) {
        var i: Int = 1
        for (key <- objMap.keySet) {
          preparedStatement.setObject(i, objMap.get(key).get)
          i += 1
        }
        preparedStatement.addBatch()
      }
      preparedStatement.executeBatch()
      //      logger.error(" table {} insert batch end time {} ",tableName,(System.currentTimeMillis()-startTime))
      true
    }
    catch {
      case e: SQLException => {
        //			Duplicate
//        logger.error(e.getMessage + " error table is:" + tableName + ",error data key is:" + map(0).get("id").getOrElse("notId"), e)
        if (e.getMessage.indexOf("Duplicate entry") < 0)
          logger.error(s"insert execute sql is ${strSql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}",e)
        throw e
      }
      case e:Throwable =>{
        logger.error(s"insert execute sql is ${strSql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}",e)
        throw e
      }
    } finally {
      preparedStatement.close()
      conn.close()
    }
  }


  def update(schema: String, tableName: String, keyword: String, map: Array[mutable.LinkedHashMap[String, java.io.Serializable]]): Boolean = {
    var conn: Connection = null
    var preparedStatement: PreparedStatement = null
    val strSql = new StringBuilder("update " + schema + "." + tableName + " set ")
    try {
      conn = sources.getDataSource("target" + schema).getConnection

      for (key <- map(0).keySet) {
        if (!keyword.split(",").contains(key))
          strSql.append(key + " = ?,")
      }
      strSql.deleteCharAt(strSql.toString.trim.length - 1)
      strSql.append(" where ")

      for (key <- keyword.split(","))
        strSql.append(" " + key + "=? and")
      strSql.delete(strSql.length - 3, strSql.length)

      if (logger.isDebugEnabled) {
        logger.debug("updateSingle sql is: {}", strSql.toString())
        logger.debug(s"update datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}")
      }
      preparedStatement = conn.prepareStatement(strSql.toString)


      //      logger.error(" table {} update batch size is {}",tableName,map.size)
      val startTime = System.currentTimeMillis()
      for (objMap <- map) {
        var i = 1
        for (objKey <- objMap.keySet) {
          if (!keyword.split(",").contains(objKey)) {
            val value = objMap.get(objKey)
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
      preparedStatement.executeBatch()
      //      logger.error(" table {} update batch end time {}",tableName,(System.currentTimeMillis()-startTime))
      true
    } catch {
      case e:Throwable =>{
        logger.error(s"update execute sql is ${strSql},insert datamap is: ${DBOperationScala.objectMapper.writeValueAsString(map)}",e)
        throw e
      }
    } finally {
      if (preparedStatement != null) preparedStatement.close()
      if (conn != null) conn.close()
    }
  }

}