package lukekafka.example

import kyo.*
import lukekafka.producer.{BrokerAck, Producer}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, RecordMetadata}

object ExampleClientProducer {
  val chunkSize = 10000
  val NumMessages = 10_000_000

  val producerConfig = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  )

  val producerBuilder: Producer < (Resource & IO) = for {
    _ <- Console.println("instantiating kafka producer")
    errorOrProducer <- Producer.makeDieInvalidConfig(producerConfig)
  } yield errorOrProducer

  def publishUsingStream(kp: Producer): Unit < (Async & Abort[Throwable]) = {
    val step1: Stream[BrokerAck, IO] = Stream.init(1 to (NumMessages / chunkSize))
      .map { index =>
        val baseIndex= index * chunkSize
        val producerRecords: IndexedSeq[ProducerRecord[Array[Byte], Array[Byte]]] = (0 to chunkSize).map { chunkIndex =>
          new ProducerRecord[Array[Byte], Array[Byte]]("escape.heartbeat", s"kyo ${baseIndex + chunkIndex}".getBytes)
        }
        val result: BrokerAck < IO = kp.produceChunkAsync(Chunk.from(producerRecords))
        result
      }
      // .buffer(2048) // Note at this point the messages are sent, and we are just waiting on promises to complete
    val step2: Stream[Chunk.Indexed[RecordMetadata], Async & Abort[Throwable]] = step1.map { brokerAck =>
//      val foo: Chunk.Indexed[RecordMetadata] < (IO & Abort[Throwable]) = Async.run(brokerAck.recordMetadata)
//      foo
      brokerAck.recordMetadata
    }

    // TODO change to runDiscard when https://github.com/getkyo/kyo/pull/673 is released
    val step3 = step2.runForeach(_ => ())

    step3
  }

  def main(args: Array[String]): Unit = {
    def timedProgram(producer: Producer) = for {
      _ <- Console.println("starting kafka publishing")
      start <- Clock.now
      _ <- publishUsingStream(producer)
      end <- Clock.now
      _ <- Console.println(s"Took ${end.toEpochMilli() - start.toEpochMilli()}ms to publish $NumMessages messages")
    } yield ()

    val programLoop: Unit < (Async & Abort[Throwable] & Resource) = for {
      producer <- producerBuilder
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
    } yield ()
    KyoApp.run(programLoop)
  }
}
