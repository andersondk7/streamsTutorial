package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{FlowStatus, SuccessfulFlow}
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommentEmitter14Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter14Spec")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

  val printCommentAsJson: Comment => FlowStatus = c => {
    val json = Json.toJson(c)
    println(json.toString())
    SuccessfulFlow("println", c.id)
  }

  val printCommentFutureAsJson: Comment => Future[FlowStatus] = c => Future {
    val json = Json.toJson(c)
    println(json.toString())
    SuccessfulFlow("println", c.id)
  }

  describe("commentEmitter") {
    it ("should print json lines to the console and to a file") {
      val stream = new CommentEmitter14(
        specials=List(4, 8, 16)
        , max=max
        , preFetch=bufferSize
        , bufferSize=bufferSize
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallel("webComments14.txt", "webComments14_special.txt")(printCommentAsJson)
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures") {
      val stream = new CommentEmitter14(
        specials=List(4, 8, 16)
        , max=max
        , preFetch=bufferSize
        , bufferSize=bufferSize
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithFutures("webComments14.future.txt", "webComments14.future._special.txt")(printCommentFutureAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (futures) in $duration\n")
    }
    it ("should print json lines to the console and to a file, using async") {
      val stream = new CommentEmitter14(
        specials=List(4, 8, 16)
        , max=max
        , preFetch=bufferSize
        , bufferSize=bufferSize
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithAsync("webCommentsFuture14.async.txt", "webComments14.async.special.txt")(printCommentAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (async) in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures and async") {
      val stream = new CommentEmitter14(
        specials=List(4, 8, 16)
        , max=max
        , preFetch=bufferSize
        , bufferSize=bufferSize
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithAsyncFuture("webComments14.async.future.txt", "webComments14.async.future.special.txt")(printCommentFutureAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (async/future) in $duration\n")
    }
  }
}
