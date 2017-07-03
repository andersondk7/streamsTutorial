package adc.tutorial.akka.streams.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Reads, Writes}

case class Comment(postId: Int
                   , id: Int
                   , name: String
                   , email: String
                   , body: String) { }

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

}
