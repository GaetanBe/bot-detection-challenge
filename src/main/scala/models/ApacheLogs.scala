package com.gaetan.bervet
package models

object ApacheLogs {

  case class AccessLogRecord (
     clientIpAddress: String,
     rfc1413ClientIdentity: String,
     remoteUser: String,
     dateTime: String,
     request: String,
     httpStatusCode: String,
     bytesSent: String,
     referer: String,
     userAgent: String
   )
}
