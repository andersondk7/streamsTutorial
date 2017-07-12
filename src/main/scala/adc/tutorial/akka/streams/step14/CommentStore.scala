package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}

import scala.collection.mutable

class CommentStore extends Actor with ActorLogging {
  import CommentStore._
  val queue = new mutable.Queue[Comment]()

  override def receive: Receive = {
    case Insert(comment) => queue += comment

    case Next =>
      if (queue.isEmpty) sender() ! NextComment()
      else sender() ! NextComment(Some(queue.dequeue()))

    case Count => sender() ! CommentCount(queue.size)
  }
}

object CommentStore {
  def props() = Props(classOf[CommentStore])
  case class Insert(comment: Comment)
  case object Next
  case object Count

  case class NextComment(comment: Option[Comment] = None)
  case class CommentCount(size: Int)
}
