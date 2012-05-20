package pl.kubiczek.randomsystem
import scala.collection.mutable.Queue
import java.net.URL
import scala.io.Source
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.Await

object RandomNumSystem {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Akka")
    val randomOrgBuffer = system.actorOf(Props[RandomOrgBuffer], "randomOrg")

    implicit val timeout = Timeout(5 seconds)
    val future = randomOrgBuffer ? RandomRequest
    val veryRandom: Int = Await.result(future.mapTo[Int], 1 minute)

    println(veryRandom)
    system.shutdown()
  }

}