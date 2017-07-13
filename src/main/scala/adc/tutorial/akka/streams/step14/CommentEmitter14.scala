package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.Flows._
import adc.tutorial.akka.streams.Graph._
import adc.tutorial.akka.streams._
import adc.tutorial.akka.streams.model._
import akka.Done
import akka.actor.{ActorSystem, Props}
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
class CommentEmitter14(specials: List[Int], max: Int, preFetch: Int, bufferSize: Int) {
  val propGenerator: SourceQueueWithComplete[Comment] => Props = q => CommentSource.props(max=max, inFlight=bufferSize, q)

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
      , bufferSize=bufferSize
      , propGenerator=propGenerator
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

    val model: Graph[ClosedShape, Future[Done]] = buildModel(
      specials
      , flowA=flowWithFuture(f)
      , flowB=flowToFileFuture(fileName)
      , flowC=flowToFileFuture(specialFileName)
      , flowD=statusReportFlow
      , bufferSize=bufferSize
      , propGenerator=propGenerator
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
    val model: Graph[ClosedShape, Future[Done]] = buildModel(
      specials
      , flowA=flowWith(f)
      , flowB=flowToFile(fileName)
      , flowC=flowToFile(specialFileName)
      , flowD=statusReportFlow
      , bufferSize=bufferSize
      , propGenerator=propGenerator
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
    val model: Graph[ClosedShape, Future[Done]] = buildModel(
      specials
     , flowA=flowWithFuture(f).async
     , flowB=flowToFileFuture(fileName).async
     , flowC=flowToFileFuture(specialFileName).async
     , flowD=statusReportFlow.async
     , bufferSize=bufferSize
     , propGenerator=propGenerator
    )
    RunnableGraph.fromGraph(model).run
  }
}

object CommentEmitter14 {






}
