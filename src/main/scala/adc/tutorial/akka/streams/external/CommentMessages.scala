package adc.tutorial.akka.streams.external

import adc.tutorial.akka.streams.model.Comment

/**
  * Messages for requesting and receiving comments from external sources.
  * <p>
  *   These are grouped here rather than in a specific implementing actor
  *   so that the consumers of the implementing actors don't depend on a
  *   specific implementation as long as the implementation can handle these
  *   messages.
  * </p>
  */
object CommentMessages {

  // -------------------------------------------------------
  // requests for comments
  // -------------------------------------------------------

  case object NextComment
  case object LastComment

  // -------------------------------------------------------
  // responses
  // -------------------------------------------------------
  case object NoMoreComments
  case class CommentResponse(comment: Comment)

}
