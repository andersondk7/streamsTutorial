package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}

import scala.collection.mutable

class CommentStore(initial: List[Comment]) extends Actor with ActorLogging {
  import CommentStore._
  val queue = new mutable.Queue[Comment]()
  queue.enqueue(initial:_*)

  override def receive: Receive = {
    case Insert(comment) =>
      queue += comment
      sender() ! Inserted(comment.id)

    case GetNext =>
      if (queue.isEmpty) sender() ! NextComment()
      else sender() ! NextComment(Some(queue.dequeue()))

    case GetCount => sender() ! CommentCount(queue.size)
  }
}

object CommentStore {
  def props(initial: List[Comment] = List()) = Props(classOf[CommentStore], initial)
  case class Insert(comment: Comment)
  case object GetNext
  case object GetCount

  case class Inserted(id: Int)
  case class NextComment(comment: Option[Comment] = None)
  case class CommentCount(size: Int)
}
