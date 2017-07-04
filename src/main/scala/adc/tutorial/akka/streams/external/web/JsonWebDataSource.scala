package adc.tutorial.akka.streams.external.web

import adc.tutorial.akka.streams.external.CommentMessages._
import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.ExecutionContext

/**
  * Retrieves data as json from a web service
  * <p>
  *   This actor queries the rest endpoint at <tt>https://jsonplaceholder.typicode.com/</tt> for <tt>Comment</tt> objects
  *   <br/>
  *   This endpoint can either retrieve all 500 comments or a single comment based on the comment id.  This class
  *   keeps an internal list of the comment ids to be retrieved and calls the endpoint for a specific id.  After
  *   the comment is retrieved it's id is removed from the list
  * <p>
  * @param max
  */
class JsonWebDataSource(max: Int) extends Actor with ActorLogging {
  import JsonWebDataSource._
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer() // needed to create the standalone wsClient
  val wsClient = StandaloneAhcWSClient()

  override def postStop(): Unit = {
    wsClient.close()
  }

  override def receive: Receive = {
    onMessage((1 to max).toList)
  }

  /**
    * message handler
    * @param ids remainng ids to retrieve
    */
  def onMessage(ids: List[Int]): Receive = {

    // -------------------------------------------------------
    // messages from parent
    // -------------------------------------------------------

    case NextComment =>
      ids.headOption.fold[Unit]( context.parent ! NoMoreComments )
      { nextId => pipe(wsClient.url(s"$url?id=$nextId").get) to self }

    case LastComment =>
      if (ids.isEmpty)  context.parent ! NoMoreComments
      else {
        pipe(wsClient.url(s"$url?id=${ids.head}").get) to self
      }

    // -------------------------------------------------------
    // response from web call
    // -------------------------------------------------------

    case response: StandaloneWSResponse => response.status match {
      case 200 =>
        Json.parse(response.body).validate[List[Comment]] match {
          case s: JsSuccess[List[Comment]] => s.get.headOption.fold(context.parent ! NoMoreComments)
            {c => {
              context.parent ! CommentResponse(c)
              context.become(onMessage(ids.tail))
            }}
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

object JsonWebDataSource {
  val url = "https://jsonplaceholder.typicode.com/comments"
  def props(max: Int) = Props(classOf[JsonWebDataSource], max)

}
