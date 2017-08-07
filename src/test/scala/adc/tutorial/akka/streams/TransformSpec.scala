package adc.tutorial.akka.streams

import adc.tutorial.akka.streams.model.Comment
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import org.scalatest.{FunSpec, Matchers}
import akka.stream.scaladsl.GraphDSL.Implicits._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class TransformSpec extends FunSpec with Matchers {
  // to handle streams
  implicit val actorSystem = ActorSystem("TransformSpec")
  implicit val materializer = ActorMaterializer()

  // to handle futures
  implicit val ec:ExecutionContext = actorSystem.dispatcher

  // Source...
  val comments: List[Comment] = List(
    Comment(postId=1 , id=1, name="Bob", email="bob@somewhere", body="This line has 5 words") // 5 words
    ,  Comment(1 , 2, name="Jane", email="jane@somewhere", body="This very long line has even more words") // 8 words
    ,  Comment(2 , 3, name="Alice", email="alice@somewhere", body="This is a comment on post #2 by Alice") // 9 words
    ,  Comment(2 , 4, name="Bob", email="bob@somewhere", body="This is also a comment on post #2 but this time by Bob") // 13 words
    ,  Comment(2 , 5, name="Jane", email="jane@somewhere", body="A second post by Jane but comments on post #2") // 10 words
  )

  // Flows (as functions) ...

  // convert a Comment to a String (the body section)
  val bodyOnly: Comment => String = (c: Comment) => c.body
  // convert a String (the body) to the number of words in the string
  val wordCount: String => Int = (s: String) => s.split("\\W+").length


  describe("a simple stream") {
    it ("should count words in the body of comments") {

      // flows.. the functions
      // Sink .. (folding over the results)
      // the stream
      // source -> flow -> flow -> sink
      // comments -> body -> count -> sum
      val result: Int = comments.map(bodyOnly(_)).map(wordCount(_)).fold(0)(_ + _)

      result shouldBe 45

    }
  }

  describe("an akka stream") {
    val source: Source[Comment, NotUsed] = Source(comments)
    val bodyOnlyFlow: Flow[Comment, String, NotUsed] = Flow[Comment].map(bodyOnly(_))
    val wordCountFlow: Flow[String, Int, NotUsed] = Flow[String].map(wordCount(_))
    val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

    it("should also count words in the body of comments") {


      val stream = source.via(bodyOnlyFlow).via(wordCountFlow).toMat(sink)(Keep.right)
      val result = Await.result(stream.run, 2 seconds)

      result shouldBe 45

    }

    it("should build and run a stream from a graph") {

      val simpleGraph: Graph[ClosedShape, Future[Int]] = {
        GraphDSL.create(sink) { implicit builder: GraphDSL.Builder[Future[Int]] =>
          s =>
            source ~> bodyOnlyFlow ~> wordCountFlow ~> s
            ClosedShape
        }
      }
      val stream = RunnableGraph.fromGraph(simpleGraph).run
      val result = Await.result(stream, 2 seconds)

      result shouldBe 45

    }
  }

}
