package adc.tutorial.akka.streams.step12

import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.ExecutionContext
import scala.util.Random
import scala.concurrent.duration._

/**
  * This class manages interactions between comment generation, storage, and processing
  */
class Director(maxDelay: Int) extends Actor with ActorLogging {

  import Director._
  implicit val ec: ExecutionContext = context.system.dispatcher

  val random = Random
  val generator =  context.actorOf(CommentGenerator.props())


  override def unhandled(message: Any): Unit = log.info(s"received $message")

  override def receive: Receive = {
    generator ! CommentGenerator.NextComment // get things going
    running
  }

  def running: Receive = {
    case CommentGenerator.Generated(id) =>
      log.info(s"generated $id")
      val next = random.nextInt(maxDelay) + 1
      context.system.scheduler.scheduleOnce(next.milliseconds,  generator, CommentGenerator.NextComment)

    case Stop => context.become(stopped)
  }

  def stopped: Receive = {
    case CommentGenerator.Generated(id) =>
      log.info(s" generated $id while stopped")
      // don't process the id
      // don't get any more

    case Stop => ; // already stopped
  }
}

object Director {
  def props(maxDelay: Int) = Props(classOf[Director], maxDelay)

  case object Stop

}


