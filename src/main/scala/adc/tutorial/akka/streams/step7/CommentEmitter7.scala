package adc.tutorial.akka.streams.step7

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{fileSink, peekMatValue}
import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
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
class CommentEmitter7(max: Int) {

  /**
    * starts the stream and executes a function on each element in the stream </li>
    *
    * @param f function to execute on each element
    * @param system actor system in which to operate
    * @return Future[Done]
    */
  def executeWith(f: (Comment) => Unit)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures

    /* see notes on <tt>IntegerEmitter5</tt> */
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=max/2, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(SourceActor7.props(max=max, queue=queue)) }
    source.runForeach(f)
  }

  /**
    *
    * @param fileName name of file to write the stream elements as json
    * @param system actor system in which to operate
    * @return Future[ Try[Done] ] since the writing to the file may fail
    */
  def executeToFile(fileName: String)(implicit system: ActorSystem): Future[Try[Done]] = {
    /** see notes on <tt>IntegerEmitter5</tt> */
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=max/2, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(SourceActor7.props(max=max, queue=queue)) }
    source.map(c => Json.toJson[Comment](c).toString()) // convert comment to json string
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }
}
