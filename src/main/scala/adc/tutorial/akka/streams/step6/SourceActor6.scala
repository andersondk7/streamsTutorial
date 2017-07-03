package adc.tutorial.akka.streams.step6

import adc.tutorial.akka.streams.model.Comment
import play.api.libs.json.Json
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.ExecutionContext

class SourceActor6(queue: SourceQueueWithComplete[Comment]) extends Actor with ActorLogging {

  import SourceActor6._
  implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    val inputs = generateComments
    pipe(queue.offer(inputs.head)) to self
    onMessage(inputs.tail)
  }
  /**
    * Message handler
    * @param values remaining elements to emit
    * @return
    */
  def onMessage(values: List[Comment]): Receive = {

    case Enqueued => // element consumed by stream, ready for another
      if (values.isEmpty) { // we've emitted all we have, mark the queue complete and die
        context.stop(self)
        queue.complete()
      }
      else { // emit the next and change state to have what is left
        context.become(onMessage(values.tail))
        pipe(queue.offer(values.head)) to self
      }

    case Dropped => // element was not consumed by the queue, send it again
      println(s"\n**********\nDropped\n")
      log.error(s"Dropped")
      pipe(queue.offer(values.head)) to self

    case QueueClosed =>
      log.error(s"QueueClosed")
      queue.complete()
      context.stop(self)

    case Failure(f) =>
      log.error(s"Failure")
      queue.fail(f)
      context.stop(self)
  }

}

object SourceActor6 {
  def props(queue: SourceQueueWithComplete[Comment]) = Props(classOf[SourceActor6], queue)

  def generateComments: List[Comment] = // lifted from https://jsonplaceholder.typicode.com/comments
    Json.parse(
    """
      |[
      | {
      |    "postId": 1,
      |    "id": 1,
      |    "name": "id labore ex et quam laborum",
      |    "email": "Eliseo@gardner.biz",
      |    "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
      |  },
      |  {
      |    "postId": 1,
      |    "id": 2,
      |    "name": "quo vero reiciendis velit similique earum",
      |    "email": "Jayne_Kuhic@sydney.com",
      |    "body": "est natus enim nihil est dolore omnis voluptatem numquam\net omnis occaecati quod ullam at\nvoluptatem error expedita pariatur\nnihil sint nostrum voluptatem reiciendis et"
      |  },
      |  {
      |    "postId": 1,
      |    "id": 3,
      |    "name": "odio adipisci rerum aut animi",
      |    "email": "Nikita@garfield.biz",
      |    "body": "quia molestiae reprehenderit quasi aspernatur\naut expedita occaecati aliquam eveniet laudantium\nomnis quibusdam delectus saepe quia accusamus maiores nam est\ncum et ducimus et vero voluptates excepturi deleniti ratione"
      |  },
      |  {
      |    "postId": 1,
      |    "id": 4,
      |    "name": "alias odio sit",
      |    "email": "Lew@alysha.tv",
      |    "body": "non et atque\noccaecati deserunt quas accusantium unde odit nobis qui voluptatem\nquia voluptas consequuntur itaque dolor\net qui rerum deleniti ut occaecati"
      |  },
      |  {
      |    "postId": 1,
      |    "id": 5,
      |    "name": "vero eaque aliquid doloribus et culpa",
      |    "email": "Hayden@althea.biz",
      |    "body": "harum non quasi et ratione\ntempore iure ex voluptates in ratione\nharum architecto fugit inventore cupiditate\nvoluptates magni quo et"
      |  },
      |  {
      |    "postId": 2,
      |    "id": 6,
      |    "name": "et fugit eligendi deleniti quidem qui sint nihil autem",
      |    "email": "Presley.Mueller@myrl.com",
      |    "body": "doloribus at sed quis culpa deserunt consectetur qui praesentium\naccusamus fugiat dicta\nvoluptatem rerum ut voluptate autem\nvoluptatem repellendus aspernatur dolorem in"
      |  },
      |  {
      |    "postId": 2,
      |    "id": 7,
      |    "name": "repellat consequatur praesentium vel minus molestias voluptatum",
      |    "email": "Dallas@ole.me",
      |    "body": "maiores sed dolores similique labore et inventore et\nquasi temporibus esse sunt id et\neos voluptatem aliquam\naliquid ratione corporis molestiae mollitia quia et magnam dolor"
      |  },
      |  {
      |    "postId": 2,
      |    "id": 8,
      |    "name": "et omnis dolorem",
      |    "email": "Mallory_Kunze@marie.org",
      |    "body": "ut voluptatem corrupti velit\nad voluptatem maiores\net nisi velit vero accusamus maiores\nvoluptates quia aliquid ullam eaque"
      |  },
      |  {
      |    "postId": 2,
      |    "id": 9,
      |    "name": "provident id voluptas",
      |    "email": "Meghan_Littel@rene.us",
      |    "body": "sapiente assumenda molestiae atque\nadipisci laborum distinctio aperiam et ab ut omnis\net occaecati aspernatur odit sit rem expedita\nquas enim ipsam minus"
      |  },
      |  {
      |    "postId": 2,
      |    "id": 10,
      |    "name": "eaque et deleniti atque tenetur ut quo ut",
      |    "email": "Carmen_Keeling@caroline.name",
      |    "body": "voluptate iusto quis nobis reprehenderit ipsum amet nulla\nquia quas dolores velit et non\naut quia necessitatibus\nnostrum quaerat nulla et accusamus nisi facilis"
      |  }
      |]
    """.stripMargin).as[List[Comment]]

}