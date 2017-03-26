import com.alibaba.otter.canal.protocol.CanalEntry.EventType
import com.hzcard.syndata.datadeal.{SchemaTableMapBatchData, SchemaTableMapData}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

/**
  * Created by zhangwei on 2017/3/6.
  */
object PartenerTest extends App {

  //  val pattern = new Regex("tp_promotion*")
  //  val matched= pattern.findAllIn("tp_promotion_offer")
  //  println(matched.length)
  //  val arrayTest = Array("1")
  //
  //  for (i <- 0 until arrayTest.length; from = i + 1; j <- from until arrayTest.length) {
  //
  //  }
  //
  //  val newArray = new ArrayBuffer[ArrayBuffer[String]]
  //  for (i <- 0 until arrayTest.length) {
  //    if (i >= {
  //      var tCount = 0
  //      for (newSg <- newArray) tCount += newSg.length
  //      tCount
  //    }) {
  //      val lastArray = newArray.takeRight(1)
  //      val tempArray = new ArrayBuffer[String]
  //      if (lastArray.length == 0)
  //        tempArray += arrayTest(i)
  //      else if (lastArray(0)(0) != arrayTest(i))
  //        tempArray += arrayTest(i)
  //      var notInsert = false
  //      var isChangLast = false
  //      val from = {
  //        if (tempArray.length == 0)
  //          i + lastArray.length + 1
  //        else
  //          i + 1
  //      }
  //      for (j <- from until arrayTest.length) {
  //        if (tempArray.length > 0) {
  //          if (!notInsert && tempArray(0) == arrayTest(j))
  //            tempArray += arrayTest(j)
  //          else if (tempArray(0) != arrayTest(j))
  //            notInsert = true
  //        } else {
  //          if (lastArray.length > 0) {
  //            if (!notInsert && lastArray(0)(0) == arrayTest(j)) {
  //              lastArray(0) += arrayTest(j)
  //              isChangLast = true
  //            } else if (!isChangLast && lastArray(0)(0) != arrayTest(j)) {
  //              tempArray += arrayTest(j)
  //            }
  //          }
  //        }
  //      }
  //      if (tempArray.length > 0)
  //        newArray += tempArray
  //    }
  //  }


  val dataArray =
    Array(SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "1")), SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "2")),
      SchemaTableMapData("schma1", "a", EventType.UPDATE, mutable.LinkedHashMap("id" -> "3")), SchemaTableMapData("schma1", "a", EventType.DELETE, mutable.LinkedHashMap("id" -> "4")),
      SchemaTableMapData("schma1", "a", EventType.DELETE, mutable.LinkedHashMap("id" -> "5")), SchemaTableMapData("schma1", "a", EventType.DELETE, mutable.LinkedHashMap("id" -> "6")),
      SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "7")), SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "8")),
      SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "9")), SchemaTableMapData("schma1", "a", EventType.INSERT, mutable.LinkedHashMap("id" -> "10"))
    )


  val newArray = new ArrayBuffer[Option[SchemaTableMapBatchData]]
  for (i <- 0 until dataArray.length) {
    //处理数据合并
    if (i >= {
      var tCount = 0
      for (newSg <- newArray) tCount += newSg.get.rowChanges.length
      tCount
    }) {
      val lastArray = newArray.takeRight(1)
      var tempNew: Option[SchemaTableMapBatchData] = None
      if (lastArray.length == 0)
        tempNew = Some(SchemaTableMapBatchData(dataArray(i).schema, dataArray(i).tableName, dataArray(i).eventType, ArrayBuffer(dataArray(i).rowChange)))
      else if (lastArray(0).get.eventType != dataArray(i).eventType)
        tempNew = Some(SchemaTableMapBatchData(dataArray(i).schema, dataArray(i).tableName, dataArray(i).eventType, ArrayBuffer(dataArray(i).rowChange)))
      var notInsert = false
      var isChangLast = false
      val from = {
        if (tempNew.isEmpty)
          i + lastArray(0).get.rowChanges.length + 1
        else
          i + 1
      }
      for (j <- from until dataArray.length) {
        if (!tempNew.isEmpty) {
          if (!notInsert && tempNew.get.eventType == dataArray(j).eventType)
            tempNew.get.rowChanges += dataArray(j).rowChange
          else if (tempNew.get.eventType != dataArray(j).eventType)
            notInsert = true
        } else {
          if (lastArray.length > 0) {
            if (!notInsert && lastArray(0).get.eventType == dataArray(j).eventType) {
              lastArray(0).get.rowChanges += dataArray(j).rowChange
              isChangLast = true
            } else if (!isChangLast && lastArray(0).get.eventType != dataArray(j).eventType) {
              tempNew = Some(SchemaTableMapBatchData(dataArray(j).schema, dataArray(j).tableName, dataArray(j).eventType, ArrayBuffer(dataArray(j).rowChange)))
            }
          }
        }
      }
      if (!tempNew.isEmpty)
        newArray += tempNew
    }
  }

  for (obj <- newArray){
    for(inObj <- obj.get.rowChanges){
      println(inObj.get("id"))
    }
  }
  newArray


}
