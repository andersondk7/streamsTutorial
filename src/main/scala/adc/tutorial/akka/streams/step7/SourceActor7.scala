package adc.tutorial.akka.streams.step7

import adc.tutorial.akka.streams.external.Messages._
import adc.tutorial.akka.streams.external.web.JsonDataSource
import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContext

class SourceActor7(max: Int, queue: SourceQueueWithComplete[Comment]) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer

  override def receive: Receive = {
    val dataSource = context.actorOf(JsonDataSource.props(max)) // delegate getting of data to the dataSource (actor)
    self ! Enqueued
    onMessage(dataSource)
  }

  def onMessage(dataSource: ActorRef): Receive = {
    // -------------------------------------------------------
    // messages from stream
    // -------------------------------------------------------

    case Enqueued => dataSource ! NextComment

    case Dropped =>
      log.error(s"Dropped")
      dataSource ! LastComment

    case QueueClosed =>
      log.error(s"QueueClosed")
      queue.complete()
      context.stop(self) // this also stops the child

    case Failure(f) =>
      log.error(s"Failure")
      queue.fail(f)
      context.stop(self) // this also stops the child

    // -------------------------------------------------------
    // messages from data store
    // -------------------------------------------------------

    case CommentResponse(comment) =>
      pipe(queue.offer(comment)) to self

    case NoMoreComments =>
      queue.complete()
      context.stop(self) // this also stops the child

  }
}

object SourceActor7 {
  def props(max: Int, queue: SourceQueueWithComplete[Comment]): Props = Props(classOf[SourceActor7], max, queue)
}
