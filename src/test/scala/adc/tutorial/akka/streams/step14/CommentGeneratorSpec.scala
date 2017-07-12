package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.step14.CommentGenerator.NextComment
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class CommentGeneratorSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("CommentGeneratorSpec")
  implicit val ec: ExecutionContext = system.dispatcher
  private val delay = 200.seconds
  private implicit val timeout: Timeout = delay

  describe("A CommentGenerator") {
    it("should generate 5 comments per post") {
      val generator = system.actorOf(CommentGenerator.props())
      val total = 15
      val comments = (1 to total).map(i => {
        val comment = Await.result( (generator ? NextComment).mapTo[Comment],delay)
        println(s"generated $comment")
        comment
      })
      val posts: Map[Int, IndexedSeq[Comment]] = comments.groupBy(c => c.postId)
      val commentsPerPost: List[Int] =  posts.values.map(g => g.size).toList
      commentsPerPost.foreach(_ shouldBe 5)
    }
  }
}
