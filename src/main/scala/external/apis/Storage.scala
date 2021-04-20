package com.gaetan.bervet
package external.apis

import scala.util.Random

import models.ApacheLogs.AccessLogRecord

object Storage {

  def storeAuthorizedLog(record: AccessLogRecord): Unit = {
    val timeToSleep: Int = Random.nextInt(1000)
    Thread.sleep(timeToSleep)
    println(s"A record regarding the ip ${record.clientIpAddress} has been stored to the authorized logs database in " +
      s"$timeToSleep milliseconds")
  }

  def storeUnAuthorizedLog(record: AccessLogRecord): Unit = {
    val timeToSleep: Int = Random.nextInt(1000)
    Thread.sleep(timeToSleep)
    println(s"A record regarding the ip ${record.clientIpAddress} has been stored to the unauthorized logs database in " +
      s"$timeToSleep milliseconds")
  }
}
