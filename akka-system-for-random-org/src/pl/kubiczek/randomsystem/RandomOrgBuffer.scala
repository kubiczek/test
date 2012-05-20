package pl.kubiczek.randomsystem
import scala.collection.mutable.Queue
import java.net.URL
import scala.io.Source
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.Props
import akka.actor.ActorRef

case object RandomRequest

class RandomOrgBuffer extends Actor with ActorLogging {

  val BatchSize = 50

  val buffer = new Queue[Int]
  val backlog = new Queue[ActorRef]
  var waitingForResponse = false

  val randomOrgClient = context.actorOf(Props[RandomOrgClient], name = "randomOrgClient")
  preFetchIfAlmostEmpty()

  def receive = {
    case RandomRequest =>
      log.info("RandomRequest received.")
      preFetchIfAlmostEmpty()
      if (buffer.isEmpty) {
        log.info("Sender pushed to backlog queue.")
        backlog += sender
      } else {
        log.info("Response with random number is send back.")
        sender ! buffer.dequeue()
      }
    case RandomOrgServerResponse(randomNumbers) =>
      log.info("Random numbers fetched succesfully: " + randomNumbers)
      buffer ++= randomNumbers
      waitingForResponse = false
      while (!backlog.isEmpty && !buffer.isEmpty) {
        log.info("Response with random number is send back to sender from backlog queue.")
        backlog.dequeue() ! buffer.dequeue()
      }
      preFetchIfAlmostEmpty()
  }

  private def preFetchIfAlmostEmpty() {
    if (buffer.size <= BatchSize / 4 && !waitingForResponse) {
      log.info("Fetching random numbers from random.org server...")
      randomOrgClient ! FetchFromRandomOrg(BatchSize)
      waitingForResponse = true
    }
  }

}