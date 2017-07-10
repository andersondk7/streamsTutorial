package adc.tutorial.akka.streams.step13



import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{FlowStatus, SuccessfulFlow}
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CommentEmitter13Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter11")
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
    it("should print json lines to the console and to a file (seed), using futures") {
      val stream = new CommentEmitter13(specials=List(14, 18, 20)
        ,max = max
        , preFetch = bufferSize
        , bufferSize = bufferSize
        , offset = 10
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallel("webComments13.seed.txt", "webComments13.seed_special.txt" )(printCommentAsJson)
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file(s) (parallel) in $duration\n")
    }
    it ("should print json lines to the console and to a file") {
      val stream = new CommentEmitter13(specials=List(134, 138, 160)
        ,max=max
        , preFetch=bufferSize
        , bufferSize=bufferSize
        , offset=120
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallel("webComments13.txt", "webComments13_special.txt")(printCommentAsJson)
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures") {
      val stream = new CommentEmitter13(specials=List(244, 248, 260),
        max=max,
        preFetch=bufferSize,
        bufferSize=bufferSize,
        offset=230
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithFutures("webComments13.future.txt", "webComments13.future._special.txt")(printCommentFutureAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (futures) in $duration\n")
    }
    it ("should print json lines to the console and to a file, using async") {
      val stream = new CommentEmitter13(
        specials=List(354, 358, 360),
        max=max,
        preFetch=bufferSize,
        bufferSize=bufferSize,
        offset=340
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithAsync("webCommentsFuture11.async.txt", "webComments13.async.special.txt")(printCommentAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (async) in $duration\n")
    }
    it ("should print json lines to the console and to a file, using futures and async") {
      val stream = new CommentEmitter13(specials=List(374, 378, 380)
        , max=max,
        preFetch=bufferSize,
        bufferSize=bufferSize,
        offset=460
      )
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream
        .executeParallelWithAsyncFuture("webComments13.async.future.txt", "webComments13.async.future.special.txt")(printCommentFutureAsJson)

      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\ncompleted: json file (async/future) in $duration\n")
    }
  }
}
