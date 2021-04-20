package com.gaetan.bervet
package external.apis

import scala.util.Random

import models.ApacheLogs.AccessLogRecord

object PredictionServer {

  def isAuthorized(log: AccessLogRecord): Boolean = {
    (log.dateTime.charAt(0) & 1) > 0
  }

  /**
   * For the sake of simplicity, this function emulates the return value of a prediction server that would identify
   * whether or not a log record is likely to no be a bot.
   * In reality, return type would be a more complex data type that would probably contains an error rate.
   * Here `host` is not used, in reality it would be the ip of a worker
   */
  def applyPredictionToBatch(host: String, recordBatch: Seq[AccessLogRecord]): Seq[(AccessLogRecord, Boolean)] = {
    val timeToSleep: Int = Random.nextInt(1000)
    Thread.sleep(timeToSleep)
    println(s"Host $host has processed ${recordBatch.length} records in $timeToSleep milliseconds")

    recordBatch.map { record =>
      (record, isAuthorized(record))
    }
  }
}
