package adc.tutorial.akka.streams.step5

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try


class IntegerEmitter5Spec extends FunSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("IntegerEmitter")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 2.seconds

  describe("integerEmitter") {
    val stream = new IntegerEmitter5(10)
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

    it ("should print factorials to a file") {
      println(s"factorials to file")
      val done: Future[Try[Done]] = stream.executeFactorialsToFile("factorial.txt")
      Await.result(done, delay)
      println("factorial file printed")
    }
  }

  override protected def afterAll(): Unit = {
    println(s"tests finished")
    system.terminate()
  }
}
