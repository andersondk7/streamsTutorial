package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.step14.Reviewer.Start
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ReviewerSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("ReviewerSpec")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds
  private implicit val timeout: Timeout = delay

  describe("A Director") {
    it("should generate comments at random intervals") {
      val director = system.actorOf(Reviewer.props(
        totalComments=25
        , fastDelayMs=5
        , slowDelayMs=30
        , partitionSize=4
      ))
      Await.result(director ? Start, delay)
      println("done")
    }
  }
}
