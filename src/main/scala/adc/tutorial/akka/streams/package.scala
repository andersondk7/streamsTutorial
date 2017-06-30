package adc.tutorial.akka

import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString

import scala.concurrent.Future

package object streams {
  def fileSink(filename: String): Sink[Any, Future[IOResult]] =
    Flow[Any]
      .map(x => ByteString(s"$x\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
}
