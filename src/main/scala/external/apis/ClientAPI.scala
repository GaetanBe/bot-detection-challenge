package com.gaetan.bervet
package external.apis

import scala.util.Random

import models.ApacheLogs

object ClientAPI {

  def notifySuspiciousLog(log: ApacheLogs.AccessLogRecord): Unit = {
    val timeToSleep: Int = Random.nextInt(10)
    Thread.sleep(timeToSleep)
    println(s"Client API has been notified in $timeToSleep milliseconds for record from ip ${log.clientIpAddress}")
  }
}
