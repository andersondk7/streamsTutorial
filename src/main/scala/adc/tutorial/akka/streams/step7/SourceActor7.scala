package adc.tutorial.akka.streams.step7

import adc.tutorial.akka.streams.external.CommentMessages._
import adc.tutorial.akka.streams.external.web.JsonWebDataSource
import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}
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
  *
  * @param max maximum number of comments to consume
  * @param queue queue that the stream will read from
  */
class SourceActor7(max: Int, queue: SourceQueueWithComplete[Comment]) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer

  private val dataSource = context.actorOf(JsonWebDataSource.props())

  override def receive: Receive = {
    //
    // delegate getting of data to the dataSource (actor), in this case it is hardcoded to a specific implementation
    // but we could generalize by passing in a Props object of the implementation that we wanted (db, queue, amazon, etc.)
    //
    dataSource ! Next // start the queue going
    onMessage(1) // set up the processing loop
  }

  /**
    * Read from the datasource and push on the queue until there are no more comments in the datasource
    */
  def onMessage(processedCount: Int): Receive = {
    // -------------------------------------------------------
    // messages from stream
    // -------------------------------------------------------

    case Enqueued =>
      if (processedCount < max) {
      dataSource ! Next
      context.become(onMessage(processedCount+1))
    }
    else {
      log.info(s"processed all $max comments")
      queue.complete()
    }

    case Dropped =>
      log.error(s"Dropped")
      dataSource ! Last

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

    case Success(comment) =>
      pipe(queue.offer(comment)) to self // put it in the queue for processing

    case Error(reason) =>
      log.error(s"could not read from queue because $reason")
      self ! Enqueued // skip processing

  }
}

object SourceActor7 {
  def props(max: Int, queue: SourceQueueWithComplete[Comment]): Props = Props(classOf[SourceActor7], max, queue)
}
