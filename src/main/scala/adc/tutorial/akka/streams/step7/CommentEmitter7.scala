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


class CommentEmitter7 {

  def executeWith(f: (Comment) => Unit)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures

    /* see notes on <tt>IntegerEmitter5</tt> */
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=5, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(SourceActor7.props(max=10, queue=queue)) }
    source.runForeach(f)
  }

  def executeToFile(fileName: String)(implicit system: ActorSystem): Future[Try[Done]] = {
    /** see notes on <tt>IntegerEmitter5</tt> */
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=5, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(SourceActor7.props(max=10, queue=queue)) }
    source.map(c => Json.toJson[Comment](c).toString()) // convert comment to json string
      .runWith(fileSink(fileName))
      .map(r => r.status)
  }
}
