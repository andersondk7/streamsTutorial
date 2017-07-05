package adc.tutorial.akka.streams.step8

import adc.tutorial.akka.streams.model.Comment
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class CommentEmitter8Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter8")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

//  describe("integerEmitter with preFetch") {
//    val stream = new CommentEmitter8(max=max, preFetch=bufferSize, bufferSize=bufferSize)
//    it ("should print json lines to a file") {
//      val start = System.currentTimeMillis()
//      val done: Future[Try[Done]] = stream.executeToFile("webComments_fetch.txt")
//      Await.result(done, delay)
//      val duration = System.currentTimeMillis() - start
//      println(s"\n\n********************\npreFetched json file printed in $duration\n")
//    }
//  }

  describe("integerEmitter with no preFetch") {
    val stream = new CommentEmitter8(max=max, preFetch=0, bufferSize=bufferSize)
    it ("should print json lines to a file") {
      val start = System.currentTimeMillis()
      val done: Future[Try[Done]] = stream.executeToFile("webComments_noFetch.txt")
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\njson file printed in $duration\n")
    }
  }

  describe("integerEmitter with preFetch") {
    val stream = new CommentEmitter8(max=max, preFetch=bufferSize, bufferSize=bufferSize)
    it ("should print json lines to a file") {
      val start = System.currentTimeMillis()
      val done: Future[Try[Done]] = stream.executeToFile("webComments_fetch.txt")
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start
      println(s"\n\n********************\npreFetched json file printed in $duration\n")
    }
  }
}
