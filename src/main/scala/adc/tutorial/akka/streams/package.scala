package adc.tutorial.akka

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import adc.tutorial.akka.streams.model.Comment
import akka.NotUsed
import akka.stream.{IOResult, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import play.api.libs.json.Json

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration

package object streams {
  /**
    * A <tt>Sink</tt> that writes the elements received into a file, one element per line
    * @param filename where to write the result
    * @return Sink
    */
  def fileSink(filename: String): Sink[Any, Future[IOResult]] =

    /**
      * this sink starts by using a 'Flow` to convert the incoming elements
      * into a ByteString so that they can be written to the file
      *
      * it then connects that flow (the ByteString of each element)
      * to a 'Sink`.
      *
      * the sink used writes each element to a file
      * in our scenario, we only want the result of writing to the file
      * so we `Keep.right`, that is the `Future[IOResult]`
      * see http://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html#reusable-pieces
      * for more details
    */
    Flow[Any]
      .map(x => ByteString(s"$x\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def printTime(label: String): Unit = println(s"$label: ${System.currentTimeMillis()}")
  /**
    * extract the source and the future of the queue
    *         shamelessly stolen from http://loicdescotte.github.io/posts/play-akka-streams-queue/
    * @param src Source[T, M]
    * @tparam T source type
    * @tparam M materialization (what gets built) in this case a Source[T]
    * @return tuple of the source and the Future[SourceQueue]
    *
    */
  def peekMatValue[T, M](src: Source[T, M]): (Source[T, M], Future[M]) = {
    val p = Promise[M]
    val s = src.mapMaterializedValue { m =>
      p.trySuccess(m)
      m
    }
    (s, p.future)
  }

  /**
    * Flow that converts a comment to json
    */
  val toJsonFlow: Flow[Comment, String, NotUsed] = Flow[Comment].map(c => Json.toJson[Comment](c).toString())

  /**
    * Flow that executes a function on each element
    * @param f function to execute
    */
  def flowWith(f: String => Unit): Flow[String, String, NotUsed] = Flow[String].map(s => {f(s); s})

  /**
    * Flow that writes each element to a file (one element per line)
    * @param fileName
    */
  def toFileFlow(fileName: String): Flow[String, Path, NotUsed] = Flow[String].map(s => {
    Files.write(Paths.get(fileName), s"$s\n".getBytes(), StandardOpenOption.APPEND)
  })
}
