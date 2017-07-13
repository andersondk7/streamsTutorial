package adc.tutorial.akka.streams

import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}

import adc.tutorial.akka.streams.model.Comment
import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Partition, Sink, Source, SourceQueueWithComplete}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

/**
  * Define the flows in the project.
  * <p>
  *   There are flows that are fixed, that is they do the same thing all the time.
  *   Examples are flows that print a message to the console
  *   or merge a fixed number of inputs to a single output
  * </p>
  * <p>
  *   There are also flows that are dynamic, that is they take a function to operate
  *   on the input to produce an output.
  * </p>
  */
object Flows {
  implicit val parallel: Int = 1 //it appears that increasing this number does not make things go faster.

  val random = new scala.util.Random
  //--------------------------------------------------------
  //   since these flows run in parallel, you can't take the output of one flow as the input to the next
  //    (as we did in step 9)
  //   rather each flow returns a FlowStatus to indicate success/failure
  //
  //  so while the input remained the same as step 9 (namely a string),
  //  the output changed from a String to a FlowStatus
  //
  //--------------------------------------------------------

  def commentReportFlow: Flow[Comment, Comment, NotUsed] = Flow[Comment].map(c => {
    println(s"processing: ${c.id}")
    c
  })

  def statusReportFlow: Flow[FlowStatus, FlowStatus, NotUsed] = Flow[FlowStatus].map( s => {
    println(s"processed: $s")
    s
  })

  def mergeCompleteFlow(count: Int): Flow[FlowStatus, (Int, Boolean), NotUsed] = {
    Flow[FlowStatus]
      .grouped(count)
      .map(s => {
        s.foldLeft(s.head.id, true) ( (result, status) => {
          if (status.id != result._1) {
            println(s"\n\nError, comments out of sync (${status.id}, ${result._1}")
            (status.id, false)
          }
          else {
            (status.id, result._2 & status.status)
          }
        } )
      })
      .map(e => {
        if (e._2) println(s"completed: ${e._1} successfully")
        else     println(s"completed: ${e._1} failure")
        e
      })
  }

  /**
    * Flow that executes a function on each element
    * @param f function to execute
    */
  def flowWith(f: Comment => FlowStatus): Flow[Comment, FlowStatus, NotUsed] = Flow[Comment].map(c => {
    f(c)
  })
  def flowWithFuture(f: Comment => Future[FlowStatus])(implicit ec: ExecutionContext): Flow[Comment, FlowStatus, NotUsed] = Flow[Comment].mapAsync[FlowStatus](parallel)(c => {
    f(c)
  })

  /**
    * Flow that writes each element to a file (one element per line)
    * @param fileName the file where the comments will be written
    */
  def flowToFile(fileName: String): Flow[Comment, FlowStatus, NotUsed] = Flow[Comment].map(c => {
    val json = Json.toJson(c)
    Files.write(Paths.get(fileName), s"${json.toString()}\n".getBytes(), StandardOpenOption.APPEND)
    SuccessfulFlow("flowToFile", c.id)
  })
  def flowToFileFuture(fileName: String)(implicit ec: ExecutionContext): Flow[Comment, FlowStatus, NotUsed] = Flow[Comment].mapAsync[FlowStatus](parallel)(c => Future{
    val json = Json.toJson(c)
    Files.write(Paths.get(fileName), s"${json.toString()}\n".getBytes(), StandardOpenOption.APPEND)
    SuccessfulFlow("flowToFile", c.id)
  })

}

/**
  * Helper to build a graph.
  */
object Graph {
  import Flows._
  import GraphDSL.Implicits._

  /**
    * Setup the file to be used in the <tt>FlowToFile</tt> type flows
    * @param fileName name of file
    */
  def setupFile(fileName: String):Unit = {
    // -------------------------------------------------
    // setup file (delete if exists, create new)
    // -------------------------------------------------
    val file = new File(fileName)
    if (file.exists()) file.delete()
    Files.createFile(Paths.get(fileName))
  }

  /**
    * Represents the parts of the graph
    * <p>
    *   This has the junctions (splitters and joiners)
    *   </br>
    *   Note that the <tt>mergeFromProcess</tt> and <tt>mergePostId</tt> do exactly the same thing,
    *   They are called out separately for ease of reading the graph
    * </p>
    * @param source input
    * @param splitByPostId splits into one of  2 flows based on postId
    * @param splitToProcess splits every comment into 2 flows
    * @param mergeFromProcess merges the comments from the process flows
    * @param mergePostId merges the comments from the different post id flows
    */
  case class Parts(
                    source: Source[Comment, SourceQueueWithComplete[Comment]]
                    , splitByPostId: UniformFanOutShape[Comment, Comment]
                    , splitToProcess: UniformFanOutShape[Comment, Comment]
                    , mergeFromProcess: UniformFanInShape[FlowStatus, FlowStatus]
                    , mergePostId: UniformFanInShape[FlowStatus, FlowStatus]
                  ) {}
  object Parts {
    /**
      * Create the parts
      * @param specials list of comment ids that are to be separated from the main flow
      * @param builder builder of the graph
      * @param bufferSize how big the stream queue should be
      * @param propGenerator creates the actor that functions as the source of the flow
      * @param system actor system in which this graph will participate
      * @return Parts
      */
    def apply(specials: List[Int]
              , builder: GraphDSL.Builder[Future[Done]]
              , bufferSize: Int
              , propGenerator: SourceQueueWithComplete[Comment] => Props)(implicit system: ActorSystem):Parts = {
      implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
      val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
      futureQueue map { queue => system.actorOf(propGenerator(queue)) }
      val byPostId = builder.add(Partition[Comment](outputPorts=2,
        partitioner = (c: Comment) => if (specials.contains(c.postId)) 1
        else 0
      )
      )
      val all = builder.add(Broadcast[Comment](2))
      val mergeAll = builder.add(Merge[FlowStatus](2))
      val mergeFinal = builder.add(Merge[FlowStatus](2))
      Parts(source, byPostId, all, mergeAll, mergeFinal)
    }
  }

  /**
    * Build a model of the graph
    * -------------------------------------------------
    *
    * the graph generally looks like: (replacing the flows with specific flow types)
    *   the lower flow (flowC) is chosen when the postId is in the 'special' list
    *   otherwise the top (FlowA/FlowB/flowD) is chosen
    *
    *                                                               / -- flowA --\
    *                                             /-- toProcess -->               | -- flowD --\
    *                                            /                  \ -- flowB --/              \
    *                                           /                                                \
    *                                          /     (non-specials)                               \
    *   source --> flowReport --> byPostId -->     ---------------------------------------------- |--> sink
    *                                          \     (specials)                                   /
    *                                           \                                                /
    *                                            \--------------------- flowC -----------------/
    *
    // -------------------------------------------------
    * @param specials list of comment ids that should be handled in the special flow
    * @param flowA see the graph layout
    * @param flowB see the graph layout
    * @param flowC see the graph layout
    * @param flowD see the graph layout
    * @param bufferSize how big the stream queue should be
    * @param propGenerator creates the actor that functions as the source of the flow
    * @return
    */
  def buildModel(specials: List[Int]
                 , flowA: Flow[Comment, FlowStatus, NotUsed]
                 , flowB: Flow[Comment, FlowStatus, NotUsed]
                 , flowC: Flow[Comment, FlowStatus, NotUsed]
                 , flowD: Flow[FlowStatus, FlowStatus, NotUsed]
                 , bufferSize: Int
                 , propGenerator: SourceQueueWithComplete[Comment] => Props
                )(implicit actorSystem: ActorSystem):  Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
    val parts = Parts.apply(specials, builder, bufferSize, propGenerator)

    parts.source ~> commentReportFlow ~> parts.splitByPostId // left side, up to the split
    parts.splitByPostId ~> parts.splitToProcess
    parts.splitToProcess ~> flowA ~> parts.mergeFromProcess  // top path
    parts.splitToProcess ~> flowB ~> parts.mergeFromProcess // bottom path
    parts.mergeFromProcess ~> flowD ~> parts.mergePostId
    parts.splitByPostId ~> flowC ~> parts.mergePostId
    parts.mergePostId ~> s // right side to the end

    ClosedShape
  }
}


