package adc.tutorial.akka.streams.step9

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
  *   this is the same actor as <tt>SourceActor8</tt>
  * </p>
  *
  * @param max maximum number of comments to consume
  * @param preFetch number of comments to pre-fetch
  * @param queue queue that the stream will read from
  */
class SourceActor9(max: Int, preFetch: Int, queue: SourceQueueWithComplete[Comment]) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer
  private val dataSource = context.actorOf(JsonWebDataSource.props())

  override def receive: Receive = {
    // start the queue going by assuming that the queue has processed a message, so get the next one
    dataSource ! Next
    // do the pre-loading
    (2 to preFetch).foreach(_ => {
      dataSource ! Next
    })
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

object SourceActor9 {
  def props(max: Int, preFetch: Int, queue: SourceQueueWithComplete[Comment]): Props = Props(classOf[SourceActor9], max, preFetch, queue)
}
