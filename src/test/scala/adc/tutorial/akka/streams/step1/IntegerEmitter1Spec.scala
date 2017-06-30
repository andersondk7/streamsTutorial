package adc.tutorial.akka.streams.step1
import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


class IntegerEmitter1Spec extends FunSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("IntegerEmitter")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  val delay = 2.seconds

  describe("integerEmitter") {
    val stream = new IntegerEmitter1(10)
    it ("should print to console") {
      val done: Future[Done] = stream.executeWith(i => println(i))
      Await.result(done, delay)
      println("integers printed")
    }
  }

  override protected def afterAll(): Unit = {
    println(s"tests finished")
    system.terminate()
  }
}
