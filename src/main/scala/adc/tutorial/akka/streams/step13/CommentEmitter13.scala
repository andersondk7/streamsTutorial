package adc.tutorial.akka.streams.step13


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
class CommentEmitter13(specials: List[Int], max: Int, preFetch: Int, bufferSize: Int, offset:Int = 0) {

  case class Parts(
                    source: Source[Comment, SourceQueueWithComplete[Comment]]
                    , splitByPostId: UniformFanOutShape[Comment, Comment]
                    , splitToProcess: UniformFanOutShape[Comment, Comment]
                    , mergeFromProcess: UniformFanInShape[FlowStatus, FlowStatus]
                    , mergePostId: UniformFanInShape[FlowStatus, FlowStatus]
                  ) {}
  object Parts {
    def apply(specials: List[Int], builder: GraphDSL.Builder[Future[Done]])(implicit system: ActorSystem):Parts = {
      implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
      val (source, futureQueue) = peekMatValue(Source.queue[Comment](bufferSize=bufferSize, overflowStrategy=OverflowStrategy.backpressure))
      futureQueue map { queue => system.actorOf(CommentSourceActor.props(max=max, preFetch=preFetch, queue=queue, offset=offset)) }
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

  private def buildModel(specials: List[Int]
                         , flowA: Flow[Comment, FlowStatus, NotUsed]
                         , flowB: Flow[Comment, FlowStatus, NotUsed]
                         , flowC: Flow[Comment, FlowStatus, NotUsed]
                         , flowD: Flow[FlowStatus, FlowStatus, NotUsed]
                        )(implicit actorSystem: ActorSystem):  Graph[ClosedShape, Future[Done]] = GraphDSL.create(Sink.ignore) { implicit builder: GraphDSL.Builder[Future[Done]] =>  s =>
    // -------------------------------------------------
    //
    // the graph generally looks like: (replacing the flows with specific flow types)
    //   the lower flow (flowC) is chosen when the postId is in the 'special' list
    //   otherwise the top (FlowA/FlowB/flowD) is chosen
    //
    //                                                               / -- flowA --\
    //                                             /-- toProcess -->               | -- flowD --\
    //                                            /                  \ -- flowB --/              \
    //                                           /                                                \
    //                                          /     (non-specials)                               \
    //   source --> flowReport --> byPostId -->     ---------------------------------------------- |--> sink
    //                                          \     (specials)                                   /
    //                                           \                                                /
    //                                            \--------------------- flowC -----------------/
    //
    // -------------------------------------------------
    val parts = Parts.apply(specials, builder)

    parts.source ~> commentReportFlow ~> parts.splitByPostId // left side, up to the split
    parts.splitByPostId ~> parts.splitToProcess
    parts.splitToProcess ~> flowA ~> parts.mergeFromProcess  // top path
    parts.splitToProcess ~> flowB ~> parts.mergeFromProcess // bottom path
    parts.mergeFromProcess ~> flowD ~> parts.mergePostId
    parts.splitByPostId ~> flowC ~> parts.mergePostId
    parts.mergePostId ~> s // right side to the end

    ClosedShape
  }

  def executeParallel(fileName: String
                      , specialFileName: String
                     )
                     (f: Comment => FlowStatus)
                     (implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    setupFile(specialFileName)

    val model: Graph[ClosedShape, Future[Done]] = buildModel(specials
      , flowA=flowWith(f)
      , flowB=flowToFile(fileName)
      , flowC=flowToFile(specialFileName)
      , flowD=statusReportFlow
    )
    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithFutures(fileName: String
                                 , specialFileName: String
                                )
                                (f: Comment => Future[FlowStatus])
                                (implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    setupFile(specialFileName)

    val model: Graph[ClosedShape, Future[Done]] = buildModel(specials
      , flowA=flowWithFuture(f)
      , flowB=flowToFileFuture(fileName)
      , flowC=flowToFileFuture(specialFileName)
      , flowD=statusReportFlow
    )
    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithAsync(fileName: String
                               , specialFileName: String
                              )
                              (f: Comment => FlowStatus)
                              (implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    setupFile(specialFileName)
    val model: Graph[ClosedShape, Future[Done]] = buildModel(specials
      , flowA=flowWith(f)
      , flowB=flowToFile(fileName)
      , flowC=flowToFile(specialFileName)
      , flowD=statusReportFlow
    )
    RunnableGraph.fromGraph(model).run
  }

  def executeParallelWithAsyncFuture(fileName: String
                                     , specialFileName: String
                                    )
                                    (f: Comment => Future[FlowStatus])
                                    (implicit system: ActorSystem): Future[Done] = {
    implicit val materializer = ActorMaterializer() // needed to create the actor(s) in the stream
    implicit val ec: ExecutionContext = system.dispatcher // needed for the futures
    setupFile(fileName)
    setupFile(specialFileName)
    val model: Graph[ClosedShape, Future[Done]] = buildModel(specials
      , flowA=flowWithFuture(f).async
      , flowB=flowToFileFuture(fileName).async
      , flowC=flowToFileFuture(specialFileName).async
      , flowD=statusReportFlow.async
    )
    RunnableGraph.fromGraph(model).run
  }
}

