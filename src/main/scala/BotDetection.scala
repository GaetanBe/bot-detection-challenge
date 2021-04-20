package com.gaetan.bervet

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import LoadBalancer.balancePredictionWorkLoad
import parsers.LogParser
import Utils._
import akka.stream.{ActorAttributes, Supervision}
import external.apis.{ClientAPI, Storage}
import models.ApacheLogs

object BotDetection  extends FailFastCirceSupport {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("BotDetection")
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    val workerIps: Seq[String] = Seq("42.236.10.125", "54.36.148.1")
    val exerciseUri: String = "http://www.almhuette-raith.at/apache-log/access.log"

    val responseFuture: Future[HttpResponse] = Http()
      .singleRequest(
        HttpRequest(
          method = HttpMethods.GET,
          uri = exerciseUri))

    responseFuture
      .map { response =>
        response
            .entity
            .withSizeLimit(100000000L)
            .dataBytes
            .map(_.utf8String)
      }
      .onComplete {
        case Success(source) =>
          source
            .flatMapConcat { response =>
              Source.repeat(response) // repeat response to simulate an unbounded stream
            }
            .flatMapConcat(response => Source(response.split("\n").toIndexedSeq))
            // Throttle the reading stream so that logs are readable
            .throttle(2, 1.seconds)
            .map { record =>
              val parsedLog: Try[ApacheLogs.AccessLogRecord] = LogParser.parseRecord(record)

              println(s"Log $parsedLog has been successfully parsed")
              parsedLog
            }
            // In reality the following line would contain a call to a dead letter queue that would
            // store and notify that a record has not been parsed successfully
            .divertTo(Sink.foreach(errorPrinter("Parsing has failed")), failed)
            .collect { case Success(successfullyParsedRecord) =>
              println(s"Parsing from ip : ${successfullyParsedRecord.clientIpAddress} has been successfully parsed")
              successfullyParsedRecord
            }
            // the following function can be used to send a metric to a database such as influxdb that would populate
            // Grafana dashboards
            // A metric could be a gauge on the rate of record processing
            .via(Utils.printRatePerSecond[ApacheLogs.AccessLogRecord](
              name = "Row per second after parsing inputs,",
              metric = _ => 1,
              outputInterval = 10.seconds,
              logger = system.log.info
            ))
            .via(balancePredictionWorkLoad(workerIps, 10))
            .flatMapConcat(responseBatch => Source(responseBatch))
            .groupBy(10, _._2)
            // The following calls would in reality return a Try and we would handle the Successes and Failures
            // appropriately
            .map { case (log: ApacheLogs.AccessLogRecord, authorized: Boolean) =>
              authorized match {
                case true => Storage.storeAuthorizedLog(log)
                case false => {
                  ClientAPI.notifySuspiciousLog(log)
                  Storage.storeUnAuthorizedLog(log)
                }
              }
            }
            .mergeSubstreams
            .withAttributes(ActorAttributes.supervisionStrategy(_ => Supervision.Restart))
            .runForeach(println)

        case Failure(e) => println(s"Request to $exerciseUri has failed with ${e.getMessage}")
      }
  }
}
