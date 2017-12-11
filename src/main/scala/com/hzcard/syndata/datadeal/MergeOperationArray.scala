package com.hzcard.syndata.datadeal

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhangwei on 2017/3/12.
  */
trait MergeOperationArray {

  def merger(esChangeArray: Array[SchemaTableMapData]) =
    esChangeArray.foldLeft(new ArrayBuffer[Option[SchemaTableMapBatchData]])((a,s)=>{
      if(a.size>0 && !a.takeRight(1)(0).isEmpty && a.takeRight(1)(0).get.eventType==s.eventType && a.takeRight(1)(0).get.schema==s.schema && a.takeRight(1)(0).get.tableName==s.tableName)
        a.takeRight(1)(0).get.rowChanges += s.rowChange
      else
        a += Some(SchemaTableMapBatchData(s.schema, s.tableName, s.eventType, ArrayBuffer(s.rowChange),s.columnTypes))
      a
    })



}
