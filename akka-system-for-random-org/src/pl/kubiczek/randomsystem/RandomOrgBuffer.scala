package pl.kubiczek.randomsystem
import scala.collection.mutable.Queue
import java.net.URL
import scala.io.Source
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive

case object RandomRequest

class RandomOrgBuffer extends Actor with ActorLogging {

  val buffer = new Queue[Int]

  def receive = LoggingReceive {
    case RandomRequest =>
      log.info("RandomRequest received by RandomOrgBuffer actor.")
      if (buffer.isEmpty) {
        buffer ++= fetchRandomNumbers(50)
      }
      sender ! buffer.dequeue()
  }

  def fetchRandomNumbers(count: Int) = {
    log.info("Fetching random numbers from random.org server...")
    val url = new URL("https://www.random.org/integers/?num=" + count + "&min=0&max=65535&col=1&base=10&format=plain&rnd=new")
    val connection = url.openConnection()
    val stream = Source.fromInputStream(connection.getInputStream)
    val randomNumbers = stream.getLines().map(_.toInt).toList
    stream.close()
    log.info("Random numbers fetched succesfully: " + randomNumbers)
    randomNumbers
  }

}