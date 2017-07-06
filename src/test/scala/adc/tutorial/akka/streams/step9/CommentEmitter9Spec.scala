package adc.tutorial.akka.streams.step9


import adc.tutorial.akka.streams.model.Comment
import akka.Done
import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class CommentEmitter9Spec extends FunSpec with Matchers with BeforeAndAfterAll{
  implicit val system = ActorSystem("CommentEmitter9")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds

  val max = 100
  val bufferSize = 10

  describe("commentEmitter") {
    val stream = new CommentEmitter9(max=max, preFetch=bufferSize, bufferSize=bufferSize)
    it ("should print json lines to the console and to a file") {
      val start = System.currentTimeMillis()
      val done: Future[Done] = stream.executeSequential("webComments9.txt")(string => println(string))
      Await.result(done, delay)
      val duration = System.currentTimeMillis() - start

      println(s"\n\n********************\njson file printed in $duration\n")
    }
  }
}