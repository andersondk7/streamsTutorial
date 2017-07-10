package adc.tutorial.akka.streams.step12


import java.io.File
import java.nio.file.{Files, Paths}

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{peekMatValue, _}
import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Works on streams of comments
  * <p>
  *   This is the wrapper class around the stream.  It has methods to perform on the stream:
  *   <ul>
  *     <li><tt>executeWith</tt> -- starts the stream and executes a function on each element in the stream </li>
  *     <li><tt>executeToFile</tt> -- starts the stream and writes the json representation of each element to a file, one element per line</li>
  *   </ul>
  * </p>
  * @param max maximum number of comments to emit per method call
  */
class CommentEmitter12(max: Int, preFetch: Int, bufferSize: Int, offset:Int = 0) {

  import Flows._
  import GraphDSL.Implicits._

  private def setupFile(fileName: String) = {
    // -------------------------------------------------
    // setup file (delete if exists, create new)
    // -------------------------------------------------
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))
  }
  private def createParts(builder: GraphDSL.Builder[Future[Done]])(implicit system: ActorSystem): (
    Source[Comment, SourceQueueWithComplete[Comment]]
    , UniformFanOutShape[Comment, Comment]
    , UniformFanInShape[FlowStatus, FlowStatus]
    ) = {
      implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
      val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
      futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue, offset=offset)) }
      val bcast = builder.add(Broadcast[Comment](2)) // split the input[Sting] into 2 outputs
      val merge = builder.add(Merge[FlowStatus](2)) // merge 2 outputs[FlowStatus] into 1 input
      (source, bcast, merge)
    }

  def executeParallel(fileName: String)(f: Comment => FlowStatus)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)

    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
      val (source, bcast, merge) = createParts(builder)

      source ~> commentReportFlow ~> bcast // left side, up to the split
      bcast ~> flowWith(f) ~> merge  // top path
      bcast ~> flowToFile(fileName) ~> merge // bottom path
      merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end

      ClosedShape
    }

    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithFutures(fileName: String)(f: Comment => Future[FlowStatus])(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)

    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
      val (source, bcast, merge) = createParts(builder)

      source ~> commentReportFlow ~> bcast // left side, up to the split
      bcast ~> flowWithFuture(f) ~> merge  // top path
      bcast ~> flowToFileFuture(fileName) ~> merge // bottom path
      merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end

      ClosedShape
    }

    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithAsync(fileName: String)(f: Comment => FlowStatus)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>
      s =>
        val (source, bcast, merge) = createParts(builder)

        source ~> commentReportFlow ~> bcast // left side, up to the split
        bcast ~> flowWith(f).async ~> merge  // top path
        bcast ~> flowToFile(fileName).async ~> merge // bottom path
        merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end

        ClosedShape
    }

    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithAsyncFuture(fileName: String)(f: Comment => Future[FlowStatus])(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>
      s =>
        val (source, bcast, merge) = createParts(builder)

        source ~> commentReportFlow ~> bcast // left side, up to the split
        bcast ~> flowWithFuture(f).async ~> merge  // top path
        bcast ~> flowToFileFuture(fileName).async ~> merge // bottom path
        merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end

        ClosedShape
    }

    RunnableGraph.fromGraph(model).run
  }

}

