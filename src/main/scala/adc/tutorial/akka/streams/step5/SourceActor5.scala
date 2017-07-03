package adc.tutorial.akka.streams.step5

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContext

/**
  * This is the actor that emits values into the queue for consumption by the stream
  * @param max number of elements to emit
  * @param queue
  */
class SourceActor5(max: Int, queue: SourceQueueWithComplete[Int]) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    // for now lets just emit all of the elements as fast as the stream can consume them
    val inputs = (1 to max).toList
    // send the first element...
    pipe(queue.offer(inputs.head)) to self
    // state is now the remaining elements
    onMessage(inputs.tail)
  }

  /**
    * Message handler
    * @param values remaining elements to emit
    * @return
    */
  def onMessage(values: List[Int]): Receive = {

    case Enqueued => // element consumed by stream, ready for another
      if (values.isEmpty) { // we've emitted all we have, mark the queue complete and die
        context.stop(self)
        queue.complete()
      }
      else { // emit the next and change state to have what is left
        context.become(onMessage(values.tail))
        pipe(queue.offer(values.head)) to self
      }

    case Dropped => // element was not consumed by the queue, send it again
      println(s"\n**********\nDropped\n")
      log.error(s"Dropped")
      pipe(queue.offer(values.head)) to self

    case QueueClosed =>
      log.error(s"QueueClosed")
      queue.complete()
      context.stop(self)

    case Failure(f) =>
      log.error(s"Failure")
      queue.fail(f)
      context.stop(self)
  }
}

object SourceActor5 {
  def props(max: Int, queue: SourceQueueWithComplete[Int]): Props = Props(classOf[SourceActor5], max, queue)
}
