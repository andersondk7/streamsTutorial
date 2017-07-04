package adc.tutorial.akka.streams.step7

import adc.tutorial.akka.streams.external.CommentMessages._
import adc.tutorial.akka.streams.external.web.JsonWebDataSource
import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContext

/**
  * Actor that puts data in the <tt>queue</tt> as the queue consumes data.
  * <p>
  *   By using a queue, that is only putting data in the queue for the stream to consume when the queue
  *   is ready to receive data, we provide <i>backpressure</t> which keeps mail boxes from overflowing and
  *   keeps the stream running.
  * </p>
  * <p>
  *   This actor uses a child actor to retrieve the data as needed.  In this instance it uses the <tt>JsonWebDataSource</tt>
  *   which retrieves comments from a web host via a rest call.
  *   <br/>
  *   Any actor could be used as long as it can handle the messages defined in <tt>CommentMessages</tt>
  * </p>
  * <p>
  *   This source ends (dies) when there are no more messages to put in the queue (that is the max messages have been
  *   put in the queue.
  * </p>
  *
  * @param max maximum number of comments to consume
  * @param queue queue that the stream will read from
  */
class SourceActor7(max: Int, queue: SourceQueueWithComplete[Comment]) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer

  override def receive: Receive = {
    //
    // delegate getting of data to the dataSource (actor), in this case it is hardcoded to a specific implementation
    // but we could generalize by passing in a Props object of the implementation that we wanted (db, queue, amazon, etc.)
    //
    val dataSource = context.actorOf(JsonWebDataSource.props(max))
    self ! Enqueued // start the queue going by assuming that the queue has processed a message, so get the next one
    onMessage(dataSource) // set up the data loop
  }

  /**
    * Read from the datasource and push on the queue until there are no more comments in the datasource
    * @param dataSource where to get the comments from
    */
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
