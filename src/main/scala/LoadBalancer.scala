package com.gaetan.bervet

import scala.concurrent.duration.DurationInt

import akka.NotUsed
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge}
import akka.stream.{FlowShape, OverflowStrategy, UniformFanInShape, UniformFanOutShape}
import models.ApacheLogs.AccessLogRecord
import external.apis.PredictionServer

object LoadBalancer {

  /**
   * Balance workflows, in an asynchronous way. Order of output elements is not guaranteed.
   */
  def balance[In, Out](workflows: Seq[Flow[In, Out, Any]]): Flow[In, Out, NotUsed] = {
    import GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      val balancer: UniformFanOutShape[In, In] = b.add(Balance[In](workflows.size, waitForAllDownstreams = true))
      val merge: UniformFanInShape[Out, Out] = b.add(Merge[Out](workflows.size))

      workflows.foreach { worker ⇒
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }

  /**
   * Balance workload over a pool of hosts, so that each model server is kept busy.
   * Process the following workflow using the first available host:
   * 1 - Use a buffer in order to keep a batch of rows ready to be sent
   * 2 - Batch `batchSize` rows or over 10 seconds
   * 3 - Get predictions from a prediction server
   */
  def balancePredictionWorkLoad(
        hosts: Seq[String],
        batchSize: Int
    ): Flow[AccessLogRecord, Seq[(AccessLogRecord, Boolean)], NotUsed] = {

    val workers: Seq[Flow[AccessLogRecord, Seq[(AccessLogRecord, Boolean)], NotUsed]] =
      hosts.map { host =>
        Flow[AccessLogRecord]
          .buffer(size = batchSize * hosts.size, OverflowStrategy.backpressure)
          .groupedWithin(batchSize, 10.seconds)
          .map { messageBatch =>
            PredictionServer.applyPredictionToBatch(host, messageBatch)
          }
      }

    LoadBalancer.balance[AccessLogRecord, Seq[(AccessLogRecord, Boolean)]](workers)
  }
}
