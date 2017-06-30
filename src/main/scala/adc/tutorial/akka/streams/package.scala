package adc.tutorial.akka

import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString

import scala.concurrent.Future

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
}
