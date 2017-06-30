package adc.tutorial.akka.streams.step2

import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._

class IntegerEmitter2(max: Int) {


  val source: Source[Int, NotUsed] = Source(1 to max)

  def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done] = {
    source.runForeach(f)
  }

  def executeFactorialsWith(f: (Any) => Unit)(implicit materializer: Materializer): Future[Done] = {
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .runForeach(f)
  }

}

