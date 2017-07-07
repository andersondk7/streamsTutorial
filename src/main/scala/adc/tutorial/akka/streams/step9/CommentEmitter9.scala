package adc.tutorial.akka.streams.step9


import java.io.File
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{fileSink, peekMatValue}
import adc.tutorial.akka.streams._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
  * Works on streams of comments
  * <p>
  *   This is the wrapper class around the stream.  It has methods to perform on the stream:
  *   <ul>
  *     <li><tt>executeWith</tt> -- starts the stream and executes a function on each element in the stream </li>
  *     <li><tt>executeToFile</tt> -- starts the stream and writes the json representation of each element to a file, one element per line</li>
  *   </ul>
  * </p>
  * @param max maximum number of comments to emit per method call
  */
class CommentEmitter9(max: Int, preFetch: Int, bufferSize: Int) {

  implicit val parallel: Int = 1 //it appears that increasing this number does not make things go faster.

  //--------------------------------------------------------
  // Flows used in the streams
  //--------------------------------------------------------
  /**
    * Flow that executes a function on each element
    * @param f function to execute
    */
  def flowWith(f: String => Unit): Flow[String, String, NotUsed] = Flow[String].map(s => {f(s); s})
  def flowWithAsync(f: String => Future[String])(implicit ec: ExecutionContext): Flow[String, String, NotUsed] = Flow[String].mapAsync[String](parallel)(s => f(s))

  /**
    * Flow that writes each element to a file (one element per line)
    * @param fileName
    */
  def flowToFile(fileName: String): Flow[String, Path, NotUsed] = Flow[String].map(s => {
    Files.write(Paths.get(fileName), s"$s\n".getBytes(), StandardOpenOption.APPEND)
  })
  def flowToFileAsync(fileName: String)(implicit ec: ExecutionContext): Flow[String, Path, NotUsed] = Flow[String].mapAsync[Path](parallel)(s => Future{
    Files.write(Paths.get(fileName), s"$s\n".getBytes(), StandardOpenOption.APPEND)
  })

  /**
    * starts the stream and executes a function on each element in the stream </li>
    *
    * @param f function to execute on each element
    * @param system actor system in which to operate
    * @return Future[Done]
    */
  def executeWith(f: Comment => Unit)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures

    /* see notes on <tt>IntegerEmitter5</tt> */
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue)) }
    source.runForeach(f)
  }

  /**
    *
    * @param fileName name of file to write the stream elements as json
    * @param system actor system in which to operate
    * @return Future[ Try[Done] ] since the writing to the file may fail
    */
  def executeToFile(fileName: String)(implicit system: ActorSystem): Future[Try[Done]] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue)) }
    source.map(c => Json.toJson[Comment](c).toString()) // convert comment to json string
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }

  def executeSequential(fileName: String)(f: String => Unit)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    // setup file (delete if exists, create new)
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))

    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue)) }


    source
      .via(toJsonFlow)
      .via(flowWith(f))
      .via(flowToFile(fileName))
      .runWith(Sink.ignore)
  }

  def executeSequentialWithFutures(fileName: String)(f: String => Future[String])(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
//    implicit val parallel: Int = 1
    // setup file (delete if exists, create new)
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))

    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue)) }


    source
      .via(toJsonFlow)
      .via(flowWithAsync(f))
      .via(flowToFileAsync(fileName))
      .runWith(Sink.ignore)
  }
}