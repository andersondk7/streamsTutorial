package adc.tutorial.akka.streams.step3

import adc.tutorial.akka.streams._
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.Try

class IntegerEmitter3(max: Int) {

  val source: Source[Int, NotUsed] = Source(1 to max)

  def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done] = {
    source.runForeach(f)
  }

  def executeFactorialsWith(f: (Any) => Unit)(implicit materializer: Materializer): Future[Done] = {
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .runForeach(f)
  }

  def executeFactorialsToFile(fileName: String)(implicit materializer: Materializer): Future[Try[Done]] = {
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .zipWith(source)( (num, index) => s"${index-1}! = $num" )
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }
}

