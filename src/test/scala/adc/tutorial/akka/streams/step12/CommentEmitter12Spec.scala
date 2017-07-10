package adc.tutorial.akka.streams.step12


import adc.tutorial.akka.streams.SuccessfulFlow
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommentEmitter12Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter11")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

  describe("commentEmitter") {
    it ("should print json lines to the console and to a file (seed), using futures") {
      val stream = new CommentEmitter12(max=max, preFetch=bufferSize, bufferSize=bufferSize, offset=10)
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallel("webCommentsFuture12a.txt")(
          c => {
            val json = Json.toJson(c)
            println(json.toString())
            SuccessfulFlow("println", c.id)
          }
        )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (parallel) in $duration\n")
    }
    it ("should print json lines to the console and to a file") {
      val stream = new CommentEmitter12(max=max, preFetch=bufferSize, bufferSize=bufferSize, offset=120)
      println(s"starting...")
      val start = System.currentTimeMillis()

      val done: Future[Done] = stream
        .executeParallel("webComments11b.txt")(
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
      val stream = new CommentEmitter12(max=max, preFetch=bufferSize, bufferSize=bufferSize, offset=230)
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithFutures("webCommentsFuture11c.txt")(
          c => Future {
            val json = Json.toJson(c)
            println(json.toString())
            SuccessfulFlow("println", c.id)
          }

        )
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (futures) in $duration\n")
    }
  }
  it ("should print json lines to the console and to a file, using async") {
    val stream = new CommentEmitter12(max=max, preFetch=bufferSize, bufferSize=bufferSize, offset=340)
    val start = System.currentTimeMillis()
    val done: Future[Done] = stream
      .executeParallelWithAsync("webCommentsFuture11d.txt")(
        c => {
          val json = Json.toJson(c)
          println(json.toString())
          SuccessfulFlow("println", c.id)
        }

      )
    Await.result(done, delay)
    val duration = System.currentTimeMillis() - start
    println(s"\n\n********************\ncompleted: json file (async) in $duration\n")
  }
  it ("should print json lines to the console and to a file, using futures and async") {
    val stream = new CommentEmitter12(max=max, preFetch=bufferSize, bufferSize=bufferSize, offset=340)
    val start = System.currentTimeMillis()
    val done: Future[Done] = stream
      .executeParallelWithAsyncFuture("webCommentsFuture11e.txt")(
        c => Future{
          val json = Json.toJson(c)
          println(json.toString())
          SuccessfulFlow("println", c.id)
        }

      )
    Await.result(done, delay)
    val duration = System.currentTimeMillis() - start
    println(s"\n\n********************\ncompleted: json file (async/future) in $duration\n")
  }
}
