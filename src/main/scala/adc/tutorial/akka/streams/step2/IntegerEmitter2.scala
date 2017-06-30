package adc.tutorial.akka.streams.step2

import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent._

class IntegerEmitter2(max: Int) {

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

  /**
    * Start (execute, run) the stream,
    * turing each element into the factorial of the element value
    * and execute the function on the factorial value
    * @param f function to be executed on each element in the stream
    * @param materializer factory used to create the actors that implement the stream and processing
    * @return A future of a <tt>Done</tt> object when the stream is exhausted
    */
  def executeFactorialsWith(f: (Any) => Unit)(implicit materializer: Materializer): Future[Done] = {
    /*
      this uses 'scan' to calculate the factorial.
      'scan' is a 'Flow' concept.

      A Flow takes input from a source and converts it into output.
      Basically it turns a 'source' of element [A] into a source of element [B]

      See the documentation on Flow (such as Flow.scan etc.) for other examples

      In this case it emits its current value which starts at '1'
      and then applies the current and next value to the given function `f`,
      emitting the next current value. In our case, the function 'f' is the factorial.

      uses BigInt to hold the potentially large factorial values
     */
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .runForeach(f)
  }

}

