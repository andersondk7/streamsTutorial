package adc.tutorial.akka.streams

import adc.tutorial.akka.streams.model.Comment
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json._

class CommentSpec extends FunSpec with Matchers {

  describe("comment") {
    it ("should read json") {
      val jsString =
        """
          |  {
          |    "postId": 1,
          |    "id": 2,
          |    "name": "id labore ex et quam laborum",
          |    "email": "Eliseo@gardner.biz",
          |    "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
          |  }
        """.stripMargin
      val json:JsValue = Json.parse(jsString)
      json.validate[Comment] match {
        case s: JsSuccess[Comment] =>
          val comment = s.get
          comment.postId shouldBe 1
          comment.index shouldBe 2
          comment.name shouldBe "id labore ex et quam laborum"
          comment.email shouldBe "Eliseo@gardner.biz"
          comment.body shouldBe "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"

        case JsError(e) => fail(s"could not parse $json because $e")
      }
    }
    it ("should read one from an array of json objects") {
      val jsString =
        """
          |[
          |  {
          |    "postId": 1,
          |    "id": 2,
          |    "name": "id labore ex et quam laborum",
          |    "email": "Eliseo@gardner.biz",
          |    "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
          |  }
          |]
        """.stripMargin
      val json:JsValue = Json.parse(jsString)
      json.validate[List[Comment]] match {
        case s: JsSuccess[List[Comment]] =>
          val comments: List[Comment] = s.get
          val comment = comments.head
          comment.postId shouldBe 1
          comment.index shouldBe 2
          comment.name shouldBe "id labore ex et quam laborum"
          comment.email shouldBe "Eliseo@gardner.biz"
          comment.body shouldBe "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"

        case JsError(e) => fail(s"could not parse $json because $e")
      }
    }

    it("should write json") {
      val comment = Comment(postId=1, id=2, name="commentName", email="comment.email@some.org", body="the comment body" )
      val json:JsValue = Json.toJson[Comment](comment)
      json.validate[Comment] match {
        case s: JsSuccess[Comment] =>
          s.get shouldBe comment
        case JsError(e) => fail(s"could not parse $json because $e")
      }
    }

    it("should read from an array") {
      val dataJson = // lifted from https://jsonplaceholder.typicode.com/comments
        Json.parse("""
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
        """.stripMargin)
      val comments: List[Comment] = dataJson.as[List[Comment]]
      comments.size shouldBe 10
    }
  }

}
