package com.hzcard.syndata.datadeal

import com.alibaba.otter.canal.protocol.CanalEntry._
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer



case class SchemaTableRowChange(schema: String, tableName: String, rowChange: RowChange)

case class SchemaTableMapData(schema: String, tableName: String, eventType: EventType, rowChange: collection.mutable.LinkedHashMap[String, java.io.Serializable],transId:String = null,primaryKeys:collection.mutable.LinkedHashMap[String, java.io.Serializable],columnTypes: collection.mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty[String, String])

case class SchemaTableMapBatchData(schema: String, tableName: String, eventType: EventType, rowChanges: ArrayBuffer[scala.collection.mutable.LinkedHashMap[String, java.io.Serializable]],columnTypes: collection.mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty[String, String])

abstract class ParentRequest(val mapDataSource: MapDataSourceLookup)

case class NotMatchMessage()

