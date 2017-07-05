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
  *   calls based on the comment id.  It cycles through all 500 comments and then starts over at comment id 1
  * <p>
  */
class JsonWebDataSource() extends Actor with ActorLogging {
  import JsonWebDataSource._
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer() // needed to create the standalone wsClient
  val wsClient = StandaloneAhcWSClient()

  override def postStop(): Unit = {
    wsClient.close()
  }

  override def receive: Receive = {
    processing(0)
  }

  /**
    * message handler
    */
  def processing(currentId:Int): Receive = {

    // -------------------------------------------------------
    // messages from parent
    // -------------------------------------------------------

    case Next => retrieveComment( if (currentId < maxComments) currentId+1 else 1 ) // define the nextId

    case Last => retrieveComment(if (currentId == 0) 1 else currentId) // use the currentId

    // -------------------------------------------------------
    // response from web call
    // -------------------------------------------------------

    case wsResponse: StandaloneWSResponse =>
      val response: Response = wsResponse.status match {
          case 200 =>
            Json.parse(wsResponse.body).validate[List[Comment]] match {
              case s: JsSuccess[List[Comment]] => s.get.headOption.fold[Response]( Error(s"received empty list") )
                                                                        {c => Success(c) }
              case JsError(e) => Error(s"could not parse result: ${wsResponse.body}")
            }
        case x => Error(s"received status $x")
    }
      context.parent ! response

  }


  private def retrieveComment(id: Int) = {
    pipe(wsClient.url(s"$url?id=$id").get) to self
    context.become(processing(id)) // update state
  }
}

object JsonWebDataSource {
  val maxComments = 500
//  case class InFlight(count: Int = 0) {
//    val completed: Boolean = count < 1
//    def more:InFlight = this.copy(count=count+1)
//    def less:InFlight = this.copy(count=count-1)
//  }

  val url = "https://jsonplaceholder.typicode.com/comments"
  def props() = Props(classOf[JsonWebDataSource])

}
