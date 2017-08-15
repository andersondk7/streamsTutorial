package adc.tutorial.akka.streams.step15

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticsearchClientUri
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class CommentEmitter15Spec extends FunSpec with Matchers {
  implicit val actorSystem = ActorSystem("CommentEmitter15Spec")

  private val delay = new FiniteDuration(50, SECONDS)

  describe("a graph with a elasticsearch sink") {
    it("should index comments") {
      val count = 200
      val uri = ElasticsearchClientUri("localhost", 9200)
      val emitter = new CommentEmitter15(webBufferSize = 50, uri = ElasticsearchClientUri("localhost", 9200), esBufferSize = 10)
      val indexed: Int = Await.result(emitter.index(count), delay)
      indexed shouldBe count
    }
  }

}
