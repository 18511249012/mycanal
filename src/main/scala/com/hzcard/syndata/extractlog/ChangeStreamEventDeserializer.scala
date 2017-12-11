package com.hzcard.syndata.extractlog

import java.sql.Timestamp
import java.util
import java.util.{Calendar, TimeZone}

import com.github.shyiko.mysql.binlog.event._
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode
import com.github.shyiko.mysql.binlog.event.deserialization._
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream
import com.hzcard.syndata.extractlog.events.{Delete, Insert, Update}

import scala.collection.JavaConverters._



class ChangestreamEventDeserializer extends {
  val tableMapData = new util.HashMap[java.lang.Long, TableMapEventData]
  private val _deserializers = new util.IdentityHashMap[EventType, EventDataDeserializer[_ <: EventData]]
  var lastQuery: Option[String] = None
} with EventDeserializer(new EventHeaderV4Deserializer, new NullEventDataDeserializer, _deserializers, tableMapData) {
  setEventDataDeserializer(EventType.QUERY, new QueryEventDataDeserializer)
  setEventDataDeserializer(EventType.TABLE_MAP, new TableMapEventDataDeserializer)
  setEventDataDeserializer(EventType.GTID, new GtidEventDataDeserializer)
  setEventDataDeserializer(EventType.XID, new XidEventDataDeserializer)

  setEventDataDeserializer(EventType.ROWS_QUERY, new RowsQueryDeserializer(this))
  setEventDataDeserializer(EventType.WRITE_ROWS, new InsertDeserializer(tableMapData,this))
  setEventDataDeserializer(EventType.UPDATE_ROWS, new UpdateDeserializer(tableMapData,this))
  setEventDataDeserializer(EventType.DELETE_ROWS, new DeleteDeserializer(tableMapData,this))
  setEventDataDeserializer(EventType.EXT_WRITE_ROWS, new InsertDeserializer(tableMapData, this,true))
  setEventDataDeserializer(EventType.EXT_UPDATE_ROWS, new UpdateDeserializer(tableMapData, this,true))
  setEventDataDeserializer(EventType.EXT_DELETE_ROWS, new DeleteDeserializer(tableMapData, this,true))

  setEventDataDeserializer(EventType.FORMAT_DESCRIPTION, new FormatDescriptionEventDataDeserializer)
  setEventDataDeserializer(EventType.ROTATE, new RotateEventDataDeserializer)
  setEventDataDeserializer(EventType.INTVAR, new IntVarEventDataDeserializer)

  @volatile protected var _nextSequenceNumber: Long = 0
  def getNextSequenceNumber(rowsInMutation: Long = 1): Long = {
    val next = _nextSequenceNumber
    _nextSequenceNumber += rowsInMutation
    next
  }

//  def main(args: Array[String]): Unit = {
//    val c = Calendar.getInstance()
//
////    c.set(Calendar.YEAR, 2017)
////    c.set(Calendar.MONTH, 11 - 1)
////    c.set(Calendar.DAY_OF_MONTH, 17)
////    c.set(Calendar.HOUR_OF_DAY, 18)
////    c.set(Calendar.MINUTE, 0)
////    c.set(Calendar.SECOND, 1)
////    c.set(Calendar.MILLISECOND, 0)
//    print(c.getTime.getTime)
//  }

  def getCurrentSequenceNumber = _nextSequenceNumber

  override def setCompatibilityMode(first: CompatibilityMode, rest: CompatibilityMode*): Unit = super.setCompatibilityMode(first, rest: _*)

}

class InsertDeserializer(
                          tableMap: util.HashMap[java.lang.Long, TableMapEventData],
                          changestreamEventDeserializer:ChangestreamEventDeserializer,
                          mayContainExtraInformation: Boolean = false

                        ) extends EventDataDeserializer[Insert] {
  private val internalDeserializer = new WriteRowsEventDataDeserializer(tableMap).
    setMayContainExtraInformation(mayContainExtraInformation)

  override def deserialize(inputStream: ByteArrayInputStream): Insert = {
    val result = internalDeserializer.deserialize(inputStream)
    val rows = result.getRows.asScala.toList

    Insert(
      result.getTableId,
      result.getIncludedColumns,
      rows,
      tableMap.get(result.getTableId).getDatabase,
      tableMap.get(result.getTableId).getTable,
      changestreamEventDeserializer.lastQuery,
      changestreamEventDeserializer.getNextSequenceNumber(rows.size)
    )
  }
}

class UpdateDeserializer(
                          tableMap: util.HashMap[java.lang.Long, TableMapEventData],
                          changestreamEventDeserializer:ChangestreamEventDeserializer,
                          mayContainExtraInformation: Boolean = false

                        ) extends EventDataDeserializer[Update] {
  private val internalDeserializer = new UpdateRowsEventDataDeserializer(tableMap).
    setMayContainExtraInformation(mayContainExtraInformation)

  override def deserialize(inputStream: ByteArrayInputStream): Update = {
    val result = internalDeserializer.deserialize(inputStream)
    // pre-processing so that we can implement a consistent interface for "rows" across all mutation types
    val (oldRows, rows) = result.getRows.asScala.toList.unzip(pair => (pair.getKey, pair.getValue))

    Update(
      result.getTableId,
      result.getIncludedColumns,
      result.getIncludedColumnsBeforeUpdate,
      rows,
      oldRows,
      tableMap.get(result.getTableId).getDatabase,
      tableMap.get(result.getTableId).getTable,
      changestreamEventDeserializer.lastQuery,
      changestreamEventDeserializer.getNextSequenceNumber(rows.size)
    )
  }
}

class DeleteDeserializer(
                          tableMap: util.HashMap[java.lang.Long, TableMapEventData],
                          changestreamEventDeserializer:ChangestreamEventDeserializer,
                          mayContainExtraInformation: Boolean = false

                        ) extends EventDataDeserializer[Delete] {
  private val internalDeserializer = new DeleteRowsEventDataDeserializer(tableMap).
    setMayContainExtraInformation(mayContainExtraInformation)

  override def deserialize(inputStream: ByteArrayInputStream): Delete = {
    val result = internalDeserializer.deserialize(inputStream)
    val rows = result.getRows.asScala.toList

    Delete(
      result.getTableId,
      result.getIncludedColumns,
      rows,
      tableMap.get(result.getTableId).getDatabase,
      tableMap.get(result.getTableId).getTable,
      changestreamEventDeserializer.lastQuery,
      changestreamEventDeserializer.getNextSequenceNumber(rows.size)
    )
  }
}

class RowsQueryDeserializer(changestreamEventDeserializer:ChangestreamEventDeserializer) extends EventDataDeserializer[EventData] {
  override def deserialize(inputStream: ByteArrayInputStream) = {
    inputStream.skip(1)
    val len = inputStream.available()
    val query = inputStream.readString(len)
    changestreamEventDeserializer.lastQuery = Some(query)

    null //scalastyle:ignore
  }
}
