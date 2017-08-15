package adc.tutorial.akka.streams.step15

import adc.tutorial.akka.streams
import adc.tutorial.akka.streams.CommentSourceActor
import adc.tutorial.akka.streams.external.elasticsearch.BulkIndexerActor
import adc.tutorial.akka.streams.model.Comment
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Graph, OverflowStrategy}
import com.sksamuel.elastic4s.ElasticsearchClientUri

import scala.concurrent.{Future, Promise}

class CommentEmitter15(webBufferSize: Int, uri: ElasticsearchClientUri, esBufferSize: Int) {

  def index(count: Int)(implicit actorSystem: ActorSystem): Future[Int] = {
    implicit val ec = actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()

    val resultCount: Promise[Int] = Promise[Int]() // will hold the eventual count of how many were indexed

    val sink: Sink[Comment, Future[Int]] = Sink.actorRefWithAck[Comment](
      ref = actorSystem.actorOf(BulkIndexerActor.props[Comment](uri, resultCount, esBufferSize, "comments", "comment", Comment.CommentElasticSearchIndex))
      , onInitMessage = BulkIndexerActor.Init
      , ackMessage = BulkIndexerActor.Ack
      , onCompleteMessage = BulkIndexerActor.Complete
    ).mapMaterializedValue(m => resultCount.future)

    val (source, futureQueue) = streams.peekMatValue(Source.queue[Comment](bufferSize=webBufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { q => actorSystem.actorOf( CommentSourceActor.props(max=count, preFetch=webBufferSize, queue=q)) }

    def reportFlow: Flow[Comment, Comment, NotUsed] = Flow[Comment].map(c => {
      println(s"processing: ${c.id}")
      c
    })

    import akka.stream.scaladsl.GraphDSL.Implicits._
    val graph: Graph[ClosedShape, Future[Int]] = GraphDSL.create(sink) { implicit builder: GraphDSL.Builder[Future[Int]] =>
      s =>
        source ~> reportFlow ~> s
        ClosedShape
    }
    RunnableGraph.fromGraph(graph).run
  }

}
