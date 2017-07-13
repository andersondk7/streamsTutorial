package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class CommentStoreSpec extends FunSpec with Matchers with BeforeAndAfterAll {
  import CommentStore._

  implicit val system = ActorSystem("CommentStoreSpec")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 5.seconds
  private implicit val timeout: Timeout = delay

  describe("A CommentStore") {
    it("should retrieve no comments when empty") {
      val commentStore = system.actorOf(CommentStore.props())
      val result: NextComment = Await.result( (commentStore ? GetNext).mapTo[NextComment], delay)
      result.comment shouldBe None
    }
    it("should read fewer than it holds") {
      val commentStore = system.actorOf(CommentStore.props())
      val total = 10
      val firstBatch = 3
      (1 to total).foreach(i => Await.result(commentStore ? Insert(Comment(i, i, "name", "email", "body")), delay))
      val initialSize = Await.result( (commentStore ? GetCount).mapTo[CommentCount].map(_.size), delay)
      initialSize shouldBe total
      val first: Seq[Int] = (1 to firstBatch).map( i => {
        Await.result( (commentStore ? GetNext).mapTo[NextComment], delay)
      }).map(_.comment.get.id)

      first shouldBe Seq(1, 2, 3)

      val next: Seq[Int] = (firstBatch+1 to total).map( i => {
        Await.result( (commentStore ? GetNext).mapTo[NextComment], delay)
      }).map(_.comment.get.id)

      next shouldBe (1 to total).drop(firstBatch)

    }
  }
}
