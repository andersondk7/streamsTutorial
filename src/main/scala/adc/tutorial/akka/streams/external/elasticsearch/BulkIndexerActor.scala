package adc.tutorial.akka.streams.external.elasticsearch

import adc.tutorial.akka.streams.model.Indexable
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.{Indexable => ESIndexable}

import scala.concurrent.{ExecutionContext, Future, Promise}

class BulkIndexerActor[T <: Indexable](
                                        client: HttpClient
                                      , complete: Promise[Int]
                                      , bufferSize: Int
                                      , indexName: String
                                      , elementType: String
                                      , implicit val indexer: ESIndexable[T]
                                      )
  extends Actor with ActorLogging{
  import BulkIndexerActor._

  implicit val ec: ExecutionContext = context.dispatcher

  override def unhandled(message: Any): Unit = {
    log.error(s"received ${message.getClass.getName}")
  }

  override def postStop(): Unit = {
    client.close()
  }


  // ********************************************************
  // initial state
  // ********************************************************
  override def receive: Receive = {

    case _: Init.type =>
      context.become(processingStream(complete, List[T](), 0))
      sender() ! Ack
  }

  // ********************************************************
  // processing stream data ...
  // ********************************************************
  def processingStream(p: Promise[Int], toBeProcessed: List[T], totalProcessed: Int): Receive = {

    case element: T =>
      val updatedBuffer = element :: toBeProcessed
      log.info(s"indexing ${element.index}, pending: ${updatedBuffer.size}/$bufferSize, total: $totalProcessed")
      if (updatedBuffer.size < bufferSize) {
        log.info("continuing")
        context.become(processingStream(p, updatedBuffer, totalProcessed+1))
        sender() ! Ack
      }
      else {
        callIndex(updatedBuffer, sender())
        context.become(waitingOnES(p, totalProcessed+1))
      }

    case _: Complete.type =>
      if (toBeProcessed.nonEmpty) {
        callIndex(toBeProcessed, sender())
        context.become(waitingOnES(p, totalProcessed, streamComplete = true))
      }
      else {
        p.success(totalProcessed)
        context.stop(self)
      }
  }

  // ********************************************************
  // waiting for ElasticSearch to complete the index...
  // ********************************************************
  def waitingOnES(p: Promise[Int], totalProcessed: Int, streamComplete: Boolean = false): Receive = {

    case ExecutionResponse(response, upstream) => // index completed

      val (remaining, newTotal) = if (response.hasFailures) {
        val reasons: List[String] = response.failures.map(i => s"$i.status} : ${i.error}").toList
        log.error(s"could not index because ${reasons.mkString("\n")} ")
        (List(), totalProcessed) // drop them for now
        // todo: implement try again, rather than just drop,
        // need to know how to map those that failed to those that were sent
      }
      else {
        //        val successCount = response.successes.size
        val successCount = bufferSize
        log.info(s"bulk request with $successCount took ${response.took} milliseconds")
        (List(), totalProcessed)
      }
      if (!streamComplete) { // do the next element (when it comes)
        log.info(s"resuming with a new total: $newTotal")
        context.become(processingStream(p, remaining, newTotal))
        upstream ! Ack
      }
      else { // no more elements, we're done
        p.success(newTotal)
        context.stop(self)
      }

    case _: Complete.type =>
      context.become(waitingOnES(p, totalProcessed, streamComplete=true))
  }

  def callIndex(toBeProcessed: List[T], upstream: ActorRef): Unit = {
    log.info(s"callIndex: toBeProcessed.size: ${toBeProcessed.size}")
    val bulkCommand: List[IndexDefinition] = toBeProcessed.map(element => index(indexName, elementType).withId(element.index).source[T](element))
    log.info(s"sending bulk request with ${bulkCommand.size} inserts")
    val execution: Future[ExecutionResponse] = client.execute { bulk (bulkCommand) }.map (r => ExecutionResponse(r, upstream))
    pipe(execution) to self
  }


}

object BulkIndexerActor {
  def props[T](
             uri: ElasticsearchClientUri
             , p: Promise[Int]
             , bufferSize: Int = 10
             , indexName: String
             , elementType: String
             , indexer: ESIndexable[T]
           ) = Props(classOf[BulkIndexerActor[T]], HttpClient(uri), p, bufferSize, indexName, elementType, indexer)
  case class ExecutionResponse(response: BulkResponse, upstream: ActorRef)

  // interacting with akka streams
  case object Init
  case object Ack
  case object Complete
}
