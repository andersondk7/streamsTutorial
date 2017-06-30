package adc.tutorial.akka.streams.step2

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class IntegerEmitter2Spec extends FunSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("IntegerEmitter")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val delay = 2.seconds

  describe("integerEmitter") {
    val stream = new IntegerEmitter2(10)
    it ("should print to console") {
      val done: Future[Done] = stream.executeWith(i => println(i))
      Await.result(done, delay)
      println("integers printed")
    }

    it ("should print factorials") {
      println(s"factorials")
      val done: Future[Done] = stream.executeFactorialsWith(i => println(i))
      Await.result(done, delay)
      println("factorials printed")
    }
  }

  override protected def afterAll(): Unit = {
    println(s"tests finished")
    system.terminate()
  }
}
