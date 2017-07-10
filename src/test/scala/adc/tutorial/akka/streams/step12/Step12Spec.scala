package adc.tutorial.akka.streams.step12

import akka.actor.ActorSystem
import akka.pattern.Patterns
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class Step12Spec extends FunSpec with Matchers {
  implicit val system = ActorSystem("CommentEmitter11")
  implicit val ec: ExecutionContext = system.dispatcher

  describe("step 12") {
    it ("should run for a fixed amount of time") {
      val delay = 1.second
      val after: Future[Int] = Patterns.after[Int]( delay, system.scheduler, ec, Future(1))

      Await.result(after, delay*2)
      println(s"done")
    }
  }

}
