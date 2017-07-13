package adc.tutorial.akka.streams.step14


import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.QueueOfferResult._
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContext

class CommentSource(max: Int
                    , inFlightMax: Int
                    , queue: SourceQueueWithComplete[Comment]
                    , initial: List[Comment] = List()
                   ) extends Actor with ActorLogging {
  import CommentStore._
  import Reviewer._
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer

  private val store = context.actorOf(CommentStore.props(initial))
  private val reviewer = context.actorOf(Reviewer.props(
    totalComments=max
  , fastDelayMs=1
  , slowDelayMs=5
  , partitionSize=5
  , Some(self)))

  context.watch(reviewer)

  override def receive: Receive =  {
    initial.foreach(c => self ! NextComment)
    reviewer ! Start
    onMessage(processedCount=0
      , inFlight=0
    )
  }

  def onMessage(processedCount: Int
                , inFlight: Int
               ): Receive =  {

    //
    // the combination of:
    //   comment
    //   Inserted
    // represent getting & storing comments from the reviewer
    //
    // this is the back pressure implementation so that we don't loose
    //   messages if they come in faster that we can process them
    //
    case c: Comment => // reviewer sent a comment
//      log.debug(s"received $c")
      store ! Insert(c)

    case Inserted(id) => // comment was stored
//      log.debug(s"dbg: Inserted: $id inFlight: $inFlight")
      if (inFlight < inFlightMax) { // stream can handle another comment, so get it
        store ! GetNext
      }

    //
    // the combination of:
    //     NextComment
    //     Enqueued
    // represent the stream side of things
    // a comment shows up to be put in the stream when:
    //     a comment was inserted and the stream has room (see Inserted)
    //     a comment was processed, which means that there is room for another (see Enqueued)
    //
    case NextComment(comment) => // retrieved comment from store, put it in the stream (if it exists)
//      log.debug(s"dbg: NextComment: $processedCount of $max inFlight: $inFlight:  $comment")
      comment.foreach(c => {
        pipe(queue.offer(c)) to self
        context.become(onMessage(processedCount, inFlight+1))
      })

    case Enqueued => // stream has processed the comment
//      log.debug(s"dbg: Enqueued: $processedCount of $max inFlight: $inFlight")
      val count = processedCount+1
      if (count < max) { // get the next comment (the response message will be NextComment)
        store ! GetNext
        context.become(onMessage(count, inFlight-1))
      }
      else {
//        log.debug(s"Source complete")
        queue.complete()
        context.stop(self)
      }

    case Dropped => log.error(s"dropped message")

    case QueueClosed =>
      log.error(s"queue closed")
      context.stop(self)

    case Failure(f) =>
      log.error(s"queue failure $f")
      queue.fail(f)
      context.stop(self)
  }
}

object CommentSource {

  def props(max: Int
            , inFlight: Int
            , queue: SourceQueueWithComplete[Comment]
            , initial: List[Comment] = List()): Props = Props(classOf[CommentSource], max, inFlight, queue, initial)

  case object CommentAvailable
}
