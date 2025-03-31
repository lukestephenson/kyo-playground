package lukekafka.example

import kyo.*
import lukekafka.producer.{BrokerAck, Producer}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, RecordMetadata}

import java.io.IOException

object ExampleClientProducer extends KyoApp {
  val chunkSize = 10000
  val NumMessages = 10_000_000

  val producerConfig = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  )

  val producerBuilder: Producer < (Resource & IO & Abort[IOException]) = for {
    _ <- Console.printLine("instantiating kafka producer")
    errorOrProducer <- Producer.makeDieInvalidConfig(producerConfig)
  } yield errorOrProducer

  def publishUsingStream(kp: Producer): Unit < (Async & Abort[Throwable]) = {
    val step1: Stream[BrokerAck, IO] = Stream.init(1 to (NumMessages / chunkSize), chunkSize = 1)
      .map { index =>
        val baseIndex= index * chunkSize
        val producerRecords: IndexedSeq[ProducerRecord[Array[Byte], Array[Byte]]] = (0 to chunkSize).map { chunkIndex =>
          new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", s"kyo ${baseIndex + chunkIndex}".getBytes)
        }
        kp.produceChunkAsync(Chunk.from(producerRecords))
      }

      // .buffer(2048) // Note at this point the messages are sent, and we are just waiting on promises to complete
    val step2: Stream[Chunk.Indexed[RecordMetadata], Async & Abort[Throwable]] = step1.map { brokerAck =>
      brokerAck.recordMetadata
    }
    step2.discard
  }

    def timedProgram(producer: Producer) = for {
      _ <- Console.printLine("starting kafka publishing")
      stopWatch <- Clock.stopwatch
      _ <- publishUsingStream(producer)
      elapsed <- stopWatch.elapsed
      _ <- Console.printLine(s"Took ${elapsed.toMillis}ms to publish $NumMessages messages")
    } yield ()

    val programLoop: Unit < (Async & Abort[Throwable] & Resource) = for {
      producer <- producerBuilder
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
    } yield ()
  run(programLoop)
}
