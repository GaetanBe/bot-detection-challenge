package com.gaetan.bervet

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._
import scala.util.{Success, Try}

object Utils {

  def getRatePerSecond[T](
        metric: T => Int = (_: T) => 1,
        outputDelay: FiniteDuration = 1.second
    ): Flow[T, Double, NotUsed] =
    Flow[T]
      .conflateWithSeed(metric(_)) { case (acc, x) ⇒ acc + metric(x) }
      .zip(Source.tick(outputDelay, outputDelay, NotUsed))
      .map(_._1.toDouble / outputDelay.toUnit(SECONDS))

  /**
   * @param name: metric's name
   * @param metric: function that counts, e.g. `_.size` to sum array's sizes over a second, default is `1`
   * @param outputInterval: interval of printed rates, e.g. `10 seconds`: will print and send metrics every 10 seconds,
   *                      default is 1 second
   * @param logger: logging method, default is `println`
   * @param registerMetric: optional function can be used to send metrics with a client, such as statsd
   */
  def printRatePerSecond[T](
        name: String,
        metric: T => Int = (_: T) => 1,
        outputInterval: FiniteDuration = 1.second,
        logger: String => Unit = println,
        registerMetric: Long => Unit = () => _
    ): Flow[T, T, NotUsed] =
    Flow[T]
      .alsoTo(
        getRatePerSecond[T](metric, outputInterval)
          .to(Sink.foreach { r =>
            logger(s"$name rate: $r")
            registerMetric(r.toLong)
          }))

  // Utility method used to test if a Try[_] succeeded or not.
  def failed[T](t: Try[T]): Boolean = t match {
    case Success(_) => false
    case _ => true
  }

  // Method that returns a println-like function
  def errorPrinter(prefix: String): Any => Unit = a => println(s"$prefix: $a")
}
