package com.hzcard.syndata.extractlog.monitor

/**
  * Created by zhangwei on 2017/6/26.
  */
object BinlogClientMonitor{
  def apply(myChannel: String): BinlogClientMonitor = new BinlogClientMonitor(myChannel)
}

class BinlogClientMonitor(myChannel:String) {

  var isRunning = false



  def initRunning = {
    //创建zookeeper path
  }



}
