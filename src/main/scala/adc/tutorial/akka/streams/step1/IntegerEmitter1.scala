package adc.tutorial.akka.streams.step1


import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._

class IntegerEmitter1(max: Int) {

  //
  // this is the stream.
  // It is a pattern or formula for a 'source' of [Int] objects
  // creating the source, does not start generating the data (that is start the stream)
  //
  // when the stream 'runs', it 'emits' the next element after the previous element is consumed
  //


  val source: Source[Int, NotUsed] = Source(1 to max)

  /**
    * Start (execute, run) the stream and execute the function on each element
    * @param f function to be executed on each element in the stream
    * @param materializer factory used to create the actors that implement the stream and processing
    * @return A future of a <tt>Done</tt> object when the stream is exhausted
    */
  def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done] = {
    source.runForeach(f) // start the stream (that is have the stream start emitting the contents), and execute 'f' on each element
  }
}

