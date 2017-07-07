package adc.tutorial.akka.streams.step10


import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}

import adc.tutorial.akka.streams.model.Comment
import adc.tutorial.akka.streams.{peekMatValue, _}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

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
class CommentEmitter10(max: Int, preFetch: Int, bufferSize: Int) {

  implicit val parallel: Int = 1 //it appears that increasing this number does not make things go faster.

  //--------------------------------------------------------
  // Flows used in the streams
  //   since these flows run in parallel, you can't take the output of one flow as the input to the next
  //    (as we did in step 9)
  //   rather each flow returns a FlowStatus to indicate success/failure
  //
  //  so while the input remained the same as step 9 (namely a string),
  //  the output changed from a String to a FlowStatus
  //
  //--------------------------------------------------------
  /**
    * Flow that executes a function on each element
    * @param f function to execute
    */
  def flowWith(f: String => FlowStatus): Flow[String, FlowStatus, NotUsed] = Flow[String].map(s => {f(s); SuccessfulFlow("flowWith")})
  def flowWithAsync(f: String => Future[FlowStatus])(implicit ec: ExecutionContext): Flow[String, FlowStatus, NotUsed] = Flow[String].mapAsync[FlowStatus](parallel)(s => f(s))

  /**
    * Flow that writes each element to a file (one element per line)
    * @param fileName the file where the comments will be written
    */
  def flowToFile(fileName: String): Flow[String, FlowStatus, NotUsed] = Flow[String].map(s => {
    Files.write(Paths.get(fileName), s"$s\n".getBytes(), StandardOpenOption.APPEND)
    SuccessfulFlow("flowToFile")
  })
  def flowToFileAsync(fileName: String)(implicit ec: ExecutionContext): Flow[String, FlowStatus, NotUsed] = Flow[String].mapAsync[FlowStatus](parallel)(s => Future{
    Files.write(Paths.get(fileName), s"$s\n".getBytes(), StandardOpenOption.APPEND)
    SuccessfulFlow("flowToFile")
  })


  def executeParallel(fileName: String)(f: String => FlowStatus)(implicit system: ActorSystem): Future[Done] = {
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
    //                        /-- flowWith(f) ------------\
    //  source --> toJson -->|                             |--> sink (ignore)
    //                        \ -- flowToFile(fileName) --/
    //
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
      import GraphDSL.Implicits._

      // the junctions... (there are 2)
      val bcast = builder.add(Broadcast[String](2)) // split the input[Sting] into 2 outputs
      val merge = builder.add(Merge[FlowStatus](2)) // merge 2 outputs[FlowStatus] into 1 input

      // the paths... there are 4 parts to the flow
      source ~> toJsonFlow ~> bcast // left side, up to the split
      bcast ~> flowWith(f) ~> merge  // top path
      bcast ~> flowToFile(fileName) ~> merge // bottom path
      merge ~> s // right side to the end
      ClosedShape
    }

    // -------------------------------------------------
    // convert the graph to a runnable graph and run it
    // -------------------------------------------------
    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithFutures(fileName: String)(f: String => Future[FlowStatus])(implicit system: ActorSystem): Future[Done] = {
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
    //                        /-- flowWithAsync(f) ------------\
    //  source --> toJson -->|                                 |--> sink (ignore)
    //                        \ -- flowToFileAsync(fileName) --/
    //
    // -------------------------------------------------
    val model: Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>
      s =>
        import GraphDSL.Implicits._

        // the junctions... (there are 2)
        val bcast = builder.add(Broadcast[String](2)) // split the input[Sting] into 2 outputs
        val merge = builder.add(Merge[FlowStatus](2)) // merge 2 outputs[FlowStatus] into 1 input

        // the paths... there are 4 parts to the flow
        source ~> toJsonFlow ~> bcast // left side, up to the split
        bcast ~> flowWithAsync(f) ~> merge  // top path
        bcast ~> flowToFileAsync(fileName) ~> merge // bottom path
        merge ~> s // right side to the end
        ClosedShape
    }

    // -------------------------------------------------
    // convert the graph to a runnable graph and run it
    // -------------------------------------------------
    RunnableGraph.fromGraph(model).run
  }

}
