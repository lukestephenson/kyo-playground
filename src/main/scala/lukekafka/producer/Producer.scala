package lukekafka.producer

import kyo.*
import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.serialization.ByteArraySerializer

import java.util
import java.util.Map as JMap
import java.util.concurrent.atomic.AtomicLong as JAtomicLong
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.control.NonFatal

trait Producer {

  def produceChunkAsync(chunk: Chunk[ProducerRecord[Array[Byte], Array[Byte]]]): BrokerAck < IO

  def produceChunkAsyncWithFailures(chunk: Chunk[ProducerRecord[Array[Byte], Array[Byte]]]): BrokerAckWithFailures < IO

}

enum InvalidConfig {
  // TODO should this be a list of all invalid keys rather than just the first?
  case IllegalConfigKey(key: String)
}

object Producer {
  private def byteArraySerializer = new ByteArraySerializer()

  def make(producerConfig: Map[String, String]): Producer < (Abort[InvalidConfig] & Resource & IO) = {
    val errorOrJavaProps: JMap[String, AnyRef] < Abort[InvalidConfig] = producerConfig.keySet.find(key => !ProducerConfig.configNames().contains(key)) match {
      case Some(invalidKey) => Abort.fail(InvalidConfig.IllegalConfigKey(invalidKey))
      case None => producerConfig.asJava
    }

    errorOrJavaProps.map { javaProps =>
      Resource.acquire(new KafkaProducer[Array[Byte], Array[Byte]](javaProps, byteArraySerializer, byteArraySerializer)).map {rawProducer =>
        Console.println("instantiated kafka producer").map(_ => new LiveProducer(rawProducer))
      }
    }
  }

  def makeDieInvalidConfig(producerConfig: Map[String, String]): Producer < (Resource & IO) = {
    // TODO if the kafka producer fails to instantiate, this results in a scala.MatchError: Panic(org.apache.kafka.common.KafkaException: Failed to construct kafka producer) here
    for {
      result <- Abort.run[InvalidConfig](make(producerConfig))
    } yield result match
        case Result.Success(liveProducer) => liveProducer
        case Result.Fail(InvalidConfig.IllegalConfigKey(key)) => throw new IllegalArgumentException(s"Invalid config key: $key")
  }
}

class LiveProducer(producer: KafkaProducer[Array[Byte], Array[Byte]]) extends Producer {

  override def produceChunkAsync(chunk: Chunk[ProducerRecord[Array[Byte], Array[Byte]]]): BrokerAck < IO = {
    produceChunkAsyncWithFailures(chunk).map { brokerAck =>
      val errorOrResults = brokerAck.recordMetadata.map { resultChunk =>
        val empty: Result[Throwable, Chunk[RecordMetadata]] = Result.Success(Chunk.empty)

        // TODO inefficient
        resultChunk.foldLeft(empty) { (acc, result) =>
          acc.flatMap { accChunk =>
            result.map { metadata =>
              accChunk.append(metadata)
            }
          }
        } match {
          case Result.Fail(err) => Abort.fail(err)
          case Result.Success(chunk) => chunk.toIndexed
        }
      }
      BrokerAck(errorOrResults)
    }
  }

  override def produceChunkAsyncWithFailures(chunk: Chunk[ProducerRecord[Array[Byte], Array[Byte]]]): BrokerAckWithFailures < IO = {
    import AllowUnsafe.embrace.danger
    for {
      serverAck <- Promise.init[Nothing, Chunk.Indexed[Result[Throwable, RecordMetadata]]]
      _ <- IO {
        val length = chunk.size
        try {
          val res = new Array[Result[Throwable, RecordMetadata]](length)
          val count = new JAtomicLong(0)
          var index = 0
          chunk.foreach { record =>
            val resultIndex = index
            index = index + 1
            producer.send(record, (metadata: RecordMetadata, err: Exception) =>
              val result = if (err != null) {
                Result.Fail(err)
              } else {
                Result.Success(metadata)
              }
              res(resultIndex) = result

              if (count.incrementAndGet == length) {
                IO.Unsafe.run(serverAck.complete(Result.Success(Chunk.from(ArraySeq.unsafeWrapArray(res))))).as(()).eval
              }
            )

            ()
          }
        } catch {
          case NonFatal(err) =>
            IO.Unsafe.run(serverAck.complete(Result.Success(Chunk.fill(length)(Result.fail(err)).toIndexed))).as(()).eval
        }
      }
    } yield BrokerAckWithFailures(serverAck.get)
  }

}
