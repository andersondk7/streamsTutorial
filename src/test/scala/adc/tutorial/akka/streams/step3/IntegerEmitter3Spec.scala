package adc.tutorial.akka.streams.step3

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IntegerEmitter3Spec extends FunSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("IntegerEmitter")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  describe("integerEmitter") {
    val stream = new IntegerEmitter3(10)
    it ("should print to console") {
      val done: Future[Done] = stream.executeWith(i => println(i))
      done.onComplete(_ => println("integers printed"))
    }

    it ("should print factorials") {
      println(s"factorials")
      val done: Future[Done] = stream.executeFactorialsWith(i => println(i))
      done.onComplete(_ => println("factorials printed"))
    }

    it ("should print factorials to a file") {
      println(s"factorials to file")
      val done: Future[Try[Done]] = stream.executeFactorialsToFile("factorial.txt")
      done.onComplete(_ => println("factorial file printed"))
    }

  }

  override protected def afterAll(): Unit = {
    println(s"tests finished")
    system.terminate()
  }
}
