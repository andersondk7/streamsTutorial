package adc.tutorial.akka.streams

import java.nio.file.{Files, Paths, StandardOpenOption}

import adc.tutorial.akka.streams.model.Comment
import akka.NotUsed
import akka.stream.scaladsl.Flow
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

object Flows {
  implicit val parallel: Int = 1 //it appears that increasing this number does not make things go faster.

  val random = new scala.util.Random
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
