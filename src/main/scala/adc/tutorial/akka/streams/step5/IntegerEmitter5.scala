package adc.tutorial.akka.streams.step5

import adc.tutorial.akka.streams.peekMatValue
import adc.tutorial.akka.streams.fileSink
import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, OverflowStrategy}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * This class is the same as in <tt>IntegerEmitter3</tt>, but uses an actor to place integers in the stream, rather than from a list
  */
class IntegerEmitter5(max: Int) {
  import IntegerEmitter5._

  /**
    * Start (execute, run) the stream and execute the function on each element
    * @param f function to be executed on each element in the stream
    * @param system actor system in which the various actors will be created
    * @return A Future of a <tt>Done</tt> object when the stream is exhausted
    */
  def executeWith(f: (Any) => Unit)(implicit system: ActorSystem):Future[Done] = {
    /* see notes on <tt>IntegerEmitter3</tt> */
    /**
      * we need a source (just like in previous IntegerEmitter classes)
      * but rather than the data for the source coming from an Iterable[Int] as before,
      * this time the data will come from a queue
      *
      * we get the source (a template that will be eventually created in the `runWith` method
      * and
      * the queue that will also be created at that time, (hence a future)
      * from the `peekMatValue` function defined in the package object `adc.tutorial.akka.streams`
      *
      * note that the size of the buffer in the queue us purposefully smaller than the number of elements that will
      * be placed in the queue, forcing back pressure on the source of the elements (our actor) to only put
      * elements in the queue when there is room in the queue
      */
    val (source, futureQueue) = peekMatValue(Source.queue[Int](bufferSize=max/2, overflowStrategy=OverflowStrategy.backpressure))

    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures

    /**
      * when the queue is created (as part of the materialization of the stream)....
      * give it to the actor that will place data in the queue
      */
    futureQueue map { queue => system.actorOf(SourceActor5.props(max, queue)) }
    source.runForeach(f)
  }

  /**
    * Start (execute, run) the stream,
    * turning each element into the factorial of the element value
    * and execute the function on the factorial value
    * @param f function to be executed on each element in the stream
    * @param system actor system in which the various actors will be created
    * @return A Future of a <tt>Done</tt> object when the stream is exhausted
    */
  def executeFactorialsWith(f: (Any) => Unit)(implicit system: ActorSystem): Future[Done] = {
    /** see notes on <tt>IntegerEmitter3</tt> */

    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    val (source, futureQueue) = peekMatValue(Source.queue[Int](bufferSize=max/2, overflowStrategy=OverflowStrategy.backpressure))

    futureQueue map { queue => system.actorOf(SourceActor5.props(max, queue)) }
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .runForeach(f)
  }

  /**
    * Generate a list of factorials from the source of [Int] and write the resulting factorials to a file
    * @param fileName name of file to write the results
    * @param system actor system in which the various actors will be created
    * @return A Future of a <tt>Try[Done]</tt> object when the stream is exhausted, since writing to the file may fail
    */
  def executeFactorialsToFile(fileName: String)(implicit system: ActorSystem): Future[Try[Done]] = {
    /** see notes on <tt>IntegerEmitter3</tt> */
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures

    val (source, futureQueue) = peekMatValue(Source.queue[Int](bufferSize=max/2, overflowStrategy=OverflowStrategy.backpressure))

    futureQueue map { queue => system.actorOf(SourceActor5.props(max, queue)) }
    source
      .scan(BigInt(1))( (acc, next) => acc * next)
      .zipWith(Source(1 to max))( (factorial, index) => s"${index-1}! = $factorial" )
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }
}

object IntegerEmitter5 {

}
