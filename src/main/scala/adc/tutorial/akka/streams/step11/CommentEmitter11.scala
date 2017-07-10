package adc.tutorial.akka.streams.step11


import java.io.File
import java.nio.file.{Files, Paths}

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{peekMatValue, _}
import akka.{Done, NotUsed}
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
class CommentEmitter11(max: Int, preFetch: Int, bufferSize: Int) {

  import Flows._


  def executeParallel(fileName: String)(f: Comment => FlowStatus)(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    // -------------------------------------------------
    // setup file (delete if exists, create new)
    // -------------------------------------------------
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))

    // -------------------------------------------------
    // create the source and queue that will be used
    // -------------------------------------------------
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue)) }

    // -------------------------------------------------
    // create the graph (remember this is a template, nothing actually gets instantiated or run at this point
    //
    //                            /-- flowWith(f) ------------\
    //  source --> flowReport -->|                            |--> statusReportFlow --> mergeCompleteFlow --> sink (ignore)
    //                            \ -- flowToFile(fileName) --/
    //
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
      import GraphDSL.Implicits._

      // the junctions... (there are 2)
      val bcast = builder.add(Broadcast[Comment](2)) // split the input[Sting] into 2 outputs
    val merge = builder.add(Merge[FlowStatus](2)) // merge 2 outputs[FlowStatus] into 1 input

      // the paths... there are 4 parts to the flow
      source ~> commentReportFlow ~> bcast // left side, up to the split
      bcast ~> flowWith(f) ~> merge  // top path
      bcast ~> flowToFile(fileName) ~> merge // bottom path
      merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end
      ClosedShape
    }

    // -------------------------------------------------
    // convert the graph to a runnable graph and run it
    // -------------------------------------------------
    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithFutures(fileName: String)(f: Comment => Future[FlowStatus])(implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    // -------------------------------------------------
    // setup file (delete if exists, create new)
    // -------------------------------------------------
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))

    // -------------------------------------------------
    // create the source and queue that will be used
    // -------------------------------------------------
    val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize = bufferSize, overflowStrategy = OverflowStrategy.backpressure))
    futureQueue map { queue => system.actorOf(CommentSourceActor.props(max = max, preFetch = preFetch, queue = queue)) }

    // -------------------------------------------------
    // create the graph
    // create the graph (remember this is a template, nothing actually gets instantiated or run at this point
    //
    //                           /-- flowWithAsync(f) ------------\
    //  source --> flowReport -->|                                |--> statusReportFlow --> mergeCompleteFlow --> sink (ignore)
    //                           \ -- flowToFileAsync(fileName) --/
    //
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>
      s =>
        import GraphDSL.Implicits._

        // the junctions... (there are 2)
      val bcast = builder.add(Broadcast[Comment](2)) // split the input[Sting] into 2 outputs
      val merge = builder.add(Merge[FlowStatus](2)) // merge 2 outputs[FlowStatus] into 1 input

        // the paths... there are 4 parts to the flow
        source ~> commentReportFlow ~> bcast // left side, up to the split
        bcast ~> flowWithFuture(f) ~> merge  // top path
        bcast ~> flowToFileFuture(fileName) ~> merge // bottom path
        merge ~> statusReportFlow ~> mergeCompleteFlow(2) ~> s // right side to the end
        ClosedShape
    }

    // -------------------------------------------------
    // convert the graph to a runnable graph and run it
    // -------------------------------------------------
    RunnableGraph.fromGraph(model).run
  }

}

