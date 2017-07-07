package adc.tutorial.akka.streams.step10


import adc.tutorial.akka.streams.SuccessfulFlow
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommentEmitter10Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter10")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

  describe("commentEmitter") {
    val stream = new CommentEmitter10(max=max, preFetch=bufferSize, bufferSize=bufferSize)
    it ("should print json lines to the console and to a file") {
      val start = System.currentTimeMillis()

      val done: Future[Done] = stream
        .executeParallel("webComments10.txt")(
                                               s => {
                                                       println(s)
                                                       SuccessfulFlow("println")
                                                     }
                                              )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start

      println(s"\n\n********************\njson file printed in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures") {
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithFutures("webCommentsFuture10.txt")(
                                                                 s => {
                                                                        Future {
                                                                                 println(s)
                                                                                 SuccessfulFlow("println")
                                                                               }
                                                                       }
                                                               )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start

      println(s"\n\n********************\njson file printed in $duration\n")
    }
  }
}
