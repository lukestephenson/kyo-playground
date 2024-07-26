package playground
import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.serialization.StringSerializer

import java.util.concurrent.Future
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava

object RawJavaKafkaProducer {
  val NumMessages = 10_000_000
  val config = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  )

  val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](config.asJava, new StringSerializer(), new StringSerializer())

  def publish(message: String): Future[RecordMetadata] = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)

    producer.send(producerRecord, new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        ()
      }
    })
  }

  def publishAll(): Unit  = {
    val futures = (1 to NumMessages).map(i => publish(s"java - $i"))

    // wait for all to be acknowledged by the server
    futures.foreach(_.get())
  }

  def main(args: Array[String]): Unit = {
//    val program: Unit < (IOs & Fibers) = publishAll())
    def timedProgram(): Unit = {
      println("starting kafka publishing")
      val start = System.currentTimeMillis()
      publishAll()
      val end = System.currentTimeMillis()
      println(s"Took ${end-start}ms to publish $NumMessages messages")
    }

    timedProgram()
    timedProgram()
    timedProgram()
    timedProgram()
    timedProgram()

  }
}
