package adc.tutorial.akka.streams.step11

import adc.tutorial.akka.streams.SuccessfulFlow
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommentEmitter11Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter11")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

  describe("commentEmitter") {
    val stream = new CommentEmitter11(max=max, preFetch=bufferSize, bufferSize=bufferSize)
    it ("should print json lines to the console and to a file") {
      val start = System.currentTimeMillis()

      val done: Future[Done] = stream
        .executeParallel("webComments11.txt")(
          c => {
            val json = Json.toJson(c)
            println(json.toString())
            SuccessfulFlow("println", c.id)
          }
        )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start

      println(s"\n\n********************\ncompleted: json file in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures") {
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithFutures("webCommentsFuture11.txt")(
          c => Future {
            val json = Json.toJson(c)
            println(json.toString())
            SuccessfulFlow("println", c.id)
          }

        )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start

      println(s"\n\n********************\ncompleted: json file (async) in $duration\n")
    }
  }
}
