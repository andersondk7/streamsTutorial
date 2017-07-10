package adc.tutorial.akka.streams.step12

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Random

/**
  * This class 'generates' comments at random
  */
class CommentGenerator() extends Actor with ActorLogging {
  import CommentGenerator._

  val random = Random

  override def receive: Receive = {
    case NextComment =>
      // pretend that the comment was generated and stored in the database
      //  for now, just select a random comment from the website (which is acting as the database)
      context.parent ! Generated(random.nextInt(499) + 1)
  }
}

object CommentGenerator {
  def props() = Props(classOf[CommentGenerator])

  case object NextComment
  case class Generated(id: Int)
}
