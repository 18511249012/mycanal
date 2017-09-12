package com.hzcard.syndata.datadeal

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhangwei on 2017/3/12.
  */
trait MergeOperationArray {

  def merger(esChangeArray: Array[SchemaTableMapData]) = {
    val newArray = new ArrayBuffer[Option[SchemaTableMapBatchData]]
    for (i <- 0 until esChangeArray.length) {
      //处理数据合并
      if (i >= {
        var tCount = 0
        for (newSg <- newArray) tCount += newSg.get.rowChanges.length
        tCount
      }) {
        val lastArray = newArray.takeRight(1)
        var tempNew: Option[SchemaTableMapBatchData] = None
        if (lastArray.length == 0 || lastArray(0).get.eventType != esChangeArray(i).eventType || lastArray(0).get.rowChanges(0).size !=  esChangeArray(i).rowChange.size || lastArray(0).get.rowChanges.length >= 1000) {
          val schema = esChangeArray(i).schema
          val tableName = esChangeArray(i).tableName
          val rowChange = esChangeArray(i).rowChange
          val nextEventType = esChangeArray(i).eventType
          tempNew = Some(SchemaTableMapBatchData(schema, tableName, nextEventType, ArrayBuffer(rowChange)))
        }
        var notInsert = false
        val from = {
          if (tempNew.isEmpty)
            i + lastArray(0).get.rowChanges.length + 1
          else
            i + 1
        }
        if (from < esChangeArray.length)
          for (j <- from until esChangeArray.length) {
            if (!notInsert) {
              if (!tempNew.isEmpty) {
                val tempNewEventType = tempNew.get.eventType
                val nextEventType = esChangeArray(j).eventType
                val rowChange = esChangeArray(j).rowChange
                try {
                  if (tempNewEventType == nextEventType && tempNew.get.rowChanges(0).size==rowChange.size  && tempNew.get.rowChanges.length < 1000)
                    tempNew.get.rowChanges += rowChange
                  else
                    notInsert = true
                } catch {
                  case e: Throwable =>
                    throw e
                }
              } else {
                if (lastArray.length > 0) {
                  val lastEventType = lastArray(0).get.eventType
                  val nextEventType = esChangeArray(j).eventType
                  val schema = esChangeArray(j).schema
                  val tableName = esChangeArray(j).tableName
                  val rowChange = esChangeArray(j).rowChange
                  try {
                    if (lastEventType == nextEventType && lastArray(0).get.rowChanges.length < 1000) {
                      lastArray(0).get.rowChanges += rowChange
                    } else {
                      tempNew = Some(SchemaTableMapBatchData(schema, tableName, nextEventType, ArrayBuffer(rowChange)))
                    }
                  } catch {
                    case e: Throwable =>
                      throw e
                  }
                }
              }
            }
          }
        if (!tempNew.isEmpty)
          newArray += tempNew
      }
    }
    newArray
  }

}
