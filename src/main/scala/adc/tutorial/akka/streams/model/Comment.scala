package adc.tutorial.akka.streams.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.{Indexable => ESIndexable}
import scala.language.implicitConversions

case class Comment(postId: Int
                   , id: Int
                   , name: String
                   , email: String
                   , body: String) extends Indexable{
  override val index:String = id.toString
}

object Comment {

  val postIdKey = "postId"
  val idKey = "id"
  val nameKey = "name"
  val emailKey = "email"
  val bodyKey = "body"
  implicit val commentReads:Reads[Comment] = (
    (JsPath \ postIdKey).read[Int] and
    (JsPath \ idKey).read[Int] and
    (JsPath \ nameKey).read[String] and
    (JsPath \ emailKey).read[String] and
    (JsPath \ bodyKey).read[String]
  ) (Comment.apply _)
  implicit val commentWrites:Writes[Comment] = (
    (JsPath \ postIdKey).write[Int] and
      (JsPath \ idKey).write[Int] and
      (JsPath \ nameKey).write[String] and
      (JsPath \ emailKey).write[String] and
      (JsPath \ bodyKey).write[String]
    ) (unlift(Comment.unapply))

  case class Generator(postId: Int, id: Int)

  //---------------------------------------------------
  // needed for elastic search interoperation
  //---------------------------------------------------
  implicit def fromSearchHit(sh: SearchHit): Comment = Json.parse(sh.sourceAsString).as[Comment]
  implicit def fromSearchHitOption(sho: Option[SearchHit]): Option[Comment] = sho.map[Comment](c => c)
  implicit object CommentElasticSearchIndex extends ESIndexable[Comment] {
    override def json(c: Comment): String = Json.toJson[Comment](c).toString()
  }

}
