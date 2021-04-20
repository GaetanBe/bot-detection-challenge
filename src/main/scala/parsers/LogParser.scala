package com.gaetan.bervet
package parsers

import scala.util.Try
import scala.util.matching.Regex

import models.ApacheLogs.AccessLogRecord

// the following code is inspired from
// https://github.com/alvinj/ScalaApacheAccessLogParser/blob/master/src/main/scala/AccessLogParser.scala
object LogParser {
   val ddd: String = "\\d{1,3}" // at least 1 but not more than 3 times (possessive)
   val ip: String = s"($ddd\\.$ddd\\.$ddd\\.$ddd)?" // like `123.456.7.89`
   val client: String = "(\\S+)" // '\S' is 'non-whitespace character'
   val user: String = "(\\S+)"
   val dateTime: String = "(\\[.+?\\])" // like `[21/Jul/2009:02:48:13 -0700]`
   val request: String = "\"(.*?)\"" // any number of any character, reluctant
   val status: String = "(\\d{3})"
   val bytes: String = "(\\S+)" // this can be a "-"
   val referer: String = "\"(.*?)\""
   val agent: String = "\"(.*?)\""
   val end: String = "\"\n\""
   val regex: Regex = s"$ip $client $user $dateTime $request $status $bytes $referer $agent".r

  def parseRecord(record: String): Try[AccessLogRecord] = {
    Try(
      record match { case regex(ip, client, user, date, request, status, bytes, referer, agent) =>
        AccessLogRecord(ip, client, user, date, request, status, bytes, referer, agent)
      }
    )
  }
}
