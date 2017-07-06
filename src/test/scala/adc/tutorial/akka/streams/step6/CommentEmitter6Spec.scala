package adc.tutorial.akka.streams.step6

import adc.tutorial.akka.streams.model.Comment
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class CommentEmitter6Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter6")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 2.seconds

  describe("commentEmitter") {
    val stream = new CommentEmitter6
    it ("should print json to console") {
      val done: Future[Done] = stream.executeWith(c => println(Json.toJson[Comment](c).toString()))
      Await.result(done, delay)
      println("json printed")
    }
    it ("should print json lines to a file") {
      val done: Future[Try[Done]] = stream.executeToFile("comments.txt")
      Await.result(done, delay)
      println("json file printed")
    }
  }
}
