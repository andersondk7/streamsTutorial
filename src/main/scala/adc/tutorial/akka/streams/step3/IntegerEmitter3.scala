package adc.tutorial.akka.streams.step3

import adc.tutorial.akka.streams._
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.Try

class IntegerEmitter3(max: Int) {
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
    * @return A Future of a <tt>Done</tt> object when the stream is exhausted
    */
  def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done] = {
    source.runForeach(f) // start the stream (that is have the stream start emitting the contents), and execute 'f' on each element
  }

  /**
    * Start (execute, run) the stream,
    * turning each element into the factorial of the element value
    * and execute the function on the factorial value
    * @param f function to be executed on each element in the stream
    * @param materializer factory used to create the actors that implement the stream and processing
    * @return A Future of a <tt>Done</tt> object when the stream is exhausted
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

  /**
    * Generate a list of factorials from the source of [Int] and write the resulting factorials to a file
    * @param fileName name of file to write the results
    * @param materializer factory used to create the actors that implement the stream and processing
    * @return A Future of a <tt>Try[Done]</tt> object when the stream is exhausted, since writing to the file may fail
    */
  def executeFactorialsToFile(fileName: String)(implicit materializer: Materializer): Future[Try[Done]] = {
    /**
      * in this scenario we use the template to create 2 identical streams,
      * one that has the numbers that will be converted into factorials
      * this becomes the 'factorial' source
      * and
      * one that uses the original numbers as an index
      * this becomes the 'index' source
      *
      * the 'zipWith' combines the two sources (factorial and index) into a single source
      * using the supplied function to create the new elements
      * (in this case by converting the factorial and index into a string
      */

    /**
      * we have also created a 'Sink'.  A sink is the termination point of a stream,
      * it is where the elements go after all the 'flows' complete.  Remember that
      * each flow executes on elements as they 'stream by'
      *
      * in this scenario the 'fileSink' has been placed in the adc.tutorial.akka.streams. package object
      * so that it can be reused.
      *
      * note that when writing to a file the result is an `Future[IOResult]` where `IOResult` is a case class
      * that has both the number of bytes written and a `Try[Done]` when the the stream is exhausted (completed)
      * in our scenario, we only care about knowing if the writing was successful so we map the Future to
      * extract the success/failure of writing
      */
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .zipWith(Source(1 to max))( (factorial, index) => s"${index-1}! = $factorial" )
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }
}

