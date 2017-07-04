package adc.tutorial.akka.streams.external

import adc.tutorial.akka.streams.model.Comment

object Messages {

  case object NextComment
  case object LastComment
  case object NoMoreComments

  case class CommentResponse(comment: Comment)

}
