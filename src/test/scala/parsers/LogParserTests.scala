package com.gaetan.bervet
package parsers

import scala.util.Try

import models.ApacheLogs
import org.scalatest._
import flatspec._
import matchers._

class LogParserTests extends AnyFlatSpec with should.Matchers {

  "parseRecord" should "return a success for value for the following records" in {
    val inputs: Seq[String] = Seq(
      """13.66.139.0 - - [19/Dec/2020:13:57:26 +0100] "GET /index.php?option=com_phocagallery&view=category&id=1:almhuette-raith&Itemid=53 HTTP/1.1" 200 32653 "-" "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)" "-"""",
      """73.166.162.225 - - [19/Dec/2020:14:58:59 +0100] "GET /apache-log/access.log HTTP/1.1" 200 1299 "-" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Safari/537.36" "-"""",
      """42.236.10.114 - - [19/Dec/2020:15:23:12 +0100] "GET /templates/_system/css/general.css HTTP/1.1" 404 239 "http://www.almhuette-raith.at/" "Mozilla/5.0 (Linux; U; Android 8.1.0; zh-CN; EML-AL00 Build/HUAWEIEML-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 baidu.sogo.uc.UCBrowser/11.9.4.974 UWS/2.13.1.48 Mobile Safari/537.36 AliApp(DingTalk/4.5.11) com.alibaba.android.rimet/10487439 Channel/227200 language/zh-CN" "-""""
    )

    val records: Seq[Try[ApacheLogs.AccessLogRecord]] = inputs.map(LogParser.parseRecord)
    val successes: Int = records.count(_.isSuccess)

    successes should be (inputs.length)
  }
}
