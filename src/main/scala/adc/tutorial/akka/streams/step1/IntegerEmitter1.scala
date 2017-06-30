package adc.tutorial.akka.streams.step1


import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._

class IntegerEmitter1(max: Int) {

  val source: Source[Int, NotUsed] = Source(1 to max)

  def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done] = {
    source.runForeach(f)
  }
}

