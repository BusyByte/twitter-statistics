package net.nomadicalien.twitter.stream

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge}

trait StreamUtils {
  def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int): Flow[In, Out, NotUsed] = {
    import akka.stream.scaladsl.GraphDSL.Implicits._

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = false))
      val merge = b.add(Merge[Out](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> worker.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })
  }
}

object StreamUtils extends StreamUtils
