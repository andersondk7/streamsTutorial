package adc.tutorial.akka.streams.external.web

import adc.tutorial.akka.streams.external.Messages._
import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.ExecutionContext

class JsonDataSource(max: Int) extends Actor with ActorLogging {
  import JsonDataSource._
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer() // needed to create the standalone wsClient
  val wsClient = StandaloneAhcWSClient()

  override def postStop(): Unit = {
    wsClient.close()
  }

  override def receive: Receive = {
    onMessage((1 to max).toList)
  }

  def onMessage(ids: List[Int]): Receive = {
    case NextComment =>
      ids.headOption.fold[Unit]( context.parent ! NoMoreComments )
      { nextId => {
                    pipe(wsClient.url(s"$url?id=$nextId").get) to self
                    context.become(onMessage(ids.tail))
                  }
      }

    case LastComment =>
      if (ids.isEmpty)  context.parent ! NoMoreComments
      else {
        pipe(wsClient.url(s"$url?id=${ids.head}").get) to self
      }

    case response: StandaloneWSResponse => response.status match {
      case 200 =>
        Json.parse(response.body).validate[List[Comment]] match {
          case s: JsSuccess[List[Comment]] => s.get.headOption.fold(context.parent ! NoMoreComments)
            {c => context.parent ! CommentResponse(c)}
          case JsError(e) =>
            log.error(s"could not parse result: ${response.body}")
            context.parent ! NoMoreComments
        }
      case x =>
        log.error(s"received status $x")
        context.parent ! NoMoreComments
    }

  }
}

object JsonDataSource {
  val url = "https://jsonplaceholder.typicode.com/comments"
  def props(max: Int) = Props(classOf[JsonDataSource], max)

}
