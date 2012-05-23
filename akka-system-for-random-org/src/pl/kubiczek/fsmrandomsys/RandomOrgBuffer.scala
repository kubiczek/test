package pl.kubiczek.fsmrandomsys

import akka.actor.Actor
import akka.actor.FSM
import akka.actor.ActorLogging
import scala.collection.mutable.Queue
import akka.actor.ActorRef
import akka.actor.Props
import pl.kubiczek.randomsystem.RandomOrgClient
import pl.kubiczek.randomsystem.FetchFromRandomOrg
import pl.kubiczek.randomsystem.RandomRequest
import pl.kubiczek.randomsystem.RandomOrgServerResponse
import akka.actor.LoggingFSM

sealed trait State
case object Active extends State
case object Buffering extends State

sealed trait Data
case class StateData(buffer: Queue[Int], backlog: Queue[ActorRef]) extends Data

class RandomOrgBuffer extends Actor with LoggingFSM[State, Data] {
  val randomOrgClient = context.actorOf(Props[RandomOrgClient], name = "randomOrgClient")
  randomOrgClient ! FetchFromRandomOrg(50)

  startWith(Buffering, StateData(Queue.empty, Queue.empty))

  when(Active) {
    case Event(RandomRequest, StateData(buffer, backlog)) =>
      handleOrQueueInBacklog(buffer, backlog)
      preFetchIfAlmostEmpty(buffer, backlog)
  }

  when(Buffering) {
    case Event(RandomRequest, StateData(buffer, backlog)) =>
      handleOrQueueInBacklog(buffer, backlog)
      stay using StateData(buffer, backlog)
    case Event(RandomOrgServerResponse(randomNumbers), StateData(buffer, backlog)) =>
      buffer ++= randomNumbers
      while (!backlog.isEmpty && !buffer.isEmpty) {
        log.info("Response with random number is send back to sender from backlog queue.")
        backlog.dequeue() ! buffer.dequeue()
      }
      preFetchIfAlmostEmpty(buffer, backlog)
  }

  initialize

  private def handleOrQueueInBacklog(buffer: Queue[Int], backlog: Queue[ActorRef]) {
    if (buffer.isEmpty) {
      log.info("Sender pushed to backlog queue.")
      backlog += sender
    } else {
      log.info("Response with random number is send back.")
      sender ! buffer.dequeue()
    }
  }

  private def preFetchIfAlmostEmpty(buffer: Queue[Int], backlog: Queue[ActorRef]) = {
    if (buffer.size <= 50 / 4) {
      randomOrgClient ! FetchFromRandomOrg(50)
      goto(Buffering) using StateData(buffer, backlog)
    } else {
      goto(Active) using StateData(buffer, backlog)
    }
  }
}