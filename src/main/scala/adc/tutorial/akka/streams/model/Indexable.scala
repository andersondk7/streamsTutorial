package adc.tutorial.akka.streams.model

/**
  * Indicates that an element can be indexed by an id
  */
trait Indexable {
  def index: String
}
