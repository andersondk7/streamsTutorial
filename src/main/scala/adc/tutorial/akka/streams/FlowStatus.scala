package adc.tutorial.akka.streams

trait FlowStatus {
  def flowName:String
  def id: Int
  def status: Boolean
  def message: Option[String]

}

case class SuccessfulFlow(override val flowName: String, override val id: Int) extends FlowStatus {
  override val status: Boolean = true
  override val message: Option[String] = None
}

case class FailedFlow(override val flowName: String, reason: String, override val id: Int) extends FlowStatus {
  override val status: Boolean = false
  override val message: Option[String] = Some(reason)
}
