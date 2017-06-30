package adc.tutorial.akka.streams.step4


import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

class IntegerEmitter4Spec extends FunSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("IntegerEmitter")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val delay = 2.seconds

  describe("integerEmitter") {
    val stream = new IntegerEmitter4(10)

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
