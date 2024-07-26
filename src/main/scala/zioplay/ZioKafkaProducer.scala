package zioplay

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import zio.{Clock, Console, RLayer, Task, ZIO, ZIOAppDefault}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.stream.ZStream
import zio._

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.*

case class KyoProducerConfig(config: Map[String, Object])

object ZioKafkaProducer extends ZIOAppDefault {
  val NumMessages = 10_000_000
  val config = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  )

  val settings: ProducerSettings = new ProducerSettings().withProperties(config)

  /**
   * Will publish until blocked on the client side
   *
   * Return a promise
   */
  def publish(kP: Producer, messages: Chunk[Int]): Task[Task[Chunk[RecordMetadata]]] = {
    val producerRecords = messages.map { i =>
      val message = s"zio - $i"
      new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", message.getBytes(StandardCharsets.UTF_8))
    }
    kP.produceChunkAsync(producerRecords)
  }

  def publishAll(): ZIO[Producer, Throwable, Unit] = {
    val program = for {
      kp <- ZIO.service[Producer]
      _ <- ZStream.range(1, NumMessages, 10_000)
        .mapChunksZIO(i => publish(kp, i).map(Chunk.single))
        .buffer(200000) // Note at this point the messages are sent, and we are just waiting on promises to complete
        .mapZIO(serverAck => serverAck)
        .runDrain
    } yield ()

    program
  }

  def run = {
    val timedProgram = for {
      _ <- Console.printLine("starting kafka publishing")
      start <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- publishAll()
      end <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- Console.printLine(s"Took ${end - start}ms to publish $NumMessages messages")
    } yield ()

    val programLoop = timedProgram *> timedProgram *>timedProgram *>timedProgram *>timedProgram
    
    programLoop.provide(ZLayer.succeed(settings), Producer.live)
  }
}
