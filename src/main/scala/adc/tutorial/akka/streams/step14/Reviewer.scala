package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * This class acts as a reviewer of posts, generating comments
  */
class Reviewer(totalComments: Int
               , fastDelayMs: Int
               , slowDelayMs: Int
               , partitionSize: Int
               , consumer: Option[ActorRef]
              ) extends Actor with ActorLogging {

  import Reviewer._
  implicit val ec: ExecutionContext = context.system.dispatcher

  private val generator =  context.actorOf(CommentGenerator.props(), "generator")

  override def unhandled(message: Any): Unit = log.warning(s"received $message from ${sender().path.name}")

  override def receive: Receive = {
    case Start =>
//      log.debug(s"will send comments to ${consumer.fold("console") {_.path.toString}}")
      generator ! CommentGenerator.NextComment // get things going
      context.become(running(totalCount=1, partitionCount=1, delayMs=fastDelayMs, sender()))
  }

  def running(totalCount: Int, partitionCount: Int, delayMs: Int, requester: ActorRef): Receive = {

    case comment: Comment =>
      if (totalCount == totalComments) {
        log.info(s"reviewer stopping after $totalCount ...")
        consumer.foreach(c => c ! comment)
        requester ! Stopped
        context.become(stopped)
      }
      else {
        val (pCount, delay) = (partitionCount, delayMs) match {
          case (`partitionSize`, `fastDelayMs`) => (1, slowDelayMs)
          case (`partitionSize`, `slowDelayMs`) => (1, fastDelayMs)
          case (p, d) => (p+1, d)
        }
        consumer.fold[Unit]( log.info(s"$pCount, $delay") ) {c => c ! comment}

        context.become(running(totalCount+1, pCount, delay, requester))
        context.system.scheduler.scheduleOnce(delay.milliseconds,  generator, CommentGenerator.NextComment)
        }
  }

  def stopped: Receive = {
    case comment: Comment =>
      log.debug(s" generated ${comment.index} while stopped")
      // don't process the id
      // don't get any more
  }
}

object Reviewer {
  def props(totalComments: Int
            , fastDelayMs: Int
            , slowDelayMs: Int
            , partitionSize: Int
           , consumer: Option[ActorRef] = None) = Props(classOf[Reviewer]
                                                  , totalComments
                                                  , fastDelayMs
                                                  , slowDelayMs
                                                  , partitionSize
                                                  , consumer
                                                )


  case object Stopped
  case object Start

}


