package playground

import kyo.*
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.*
import scala.jdk.javaapi.FutureConverters
import java.util.concurrent.atomic.AtomicLong as JAtomicLong
import scala.util.control.NonFatal

case class KyoProducerConfig(config: Map[String, Object])

object KyoKafkaProducer {
  val NumMessages = 10_000_000
  val config = KyoProducerConfig(Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  ))

  val producerBuilder: KafkaProducer[String, String] < (Envs[KyoProducerConfig] & IOs & Consoles) = for {
    _ <- Consoles.println("instantiating kafka producer")
    producerConfig <- Envs.get[KyoProducerConfig]
    javaProps = producerConfig.config.asJava
    p <- IOs(new KafkaProducer[String, String](javaProps, new StringSerializer(), new StringSerializer()))
  } yield p

  /**
   * Will publish until blocked on the client side
   *
   * Return a promise
   */
  def publishSecondAttempt(kP: KafkaProducer[String, String], message: String): Fiber[Try[RecordMetadata]] < IOs = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
    //    println(s"sending $message")

    for {
      promise <- IOs(scala.concurrent.Promise[Try[RecordMetadata]]())
      _ <- IOs(kP.send(producerRecord, new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          // TODO don't use Try, but use kyo attempt for lower overhead
          if (exception != null) {
            //            println(s"got failure callback - $message")
            promise.success(Failure(exception))
          } else {
            //                        println(s"got success callback - $message")
            promise.success(Success(metadata))
          }
        }
      }))
      serverAck <- Fibers.fromFutureFiber(promise.future)
    } yield serverAck
  }

  def publish(kP: KafkaProducer[String, String], message: String): Promise[Try[RecordMetadata]] < IOs = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
    //    println(s"sending $message")
    for {
      serverAck <- Fibers.initPromise[Try[RecordMetadata]]
      - <- IOs(kP.send(producerRecord, new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          // TODO don't use Try, but use kyo attempt for lower overhead
          if (exception != null) {
            IOs.runLazy(serverAck.complete(Failure(exception)))
          } else {
            //            println("got success callback - $message")
            IOs.runLazy(serverAck.complete(Success(metadata)))
          }
        }
      }))
    } yield serverAck
  }

  def publishWithAttempt(kP: KafkaProducer[String, String], message: String): Promise[RecordMetadata < Aborts[Exception]] < IOs = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
    //    println(s"sending $message")
    for {
      serverAck <- Fibers.initPromise[RecordMetadata < Aborts[Exception]]
      - <- IOs(kP.send(producerRecord, new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          // TODO don't use Try, but use kyo attempt for lower overhead
          if (exception != null) {
            IOs.run(serverAck.complete(Aborts.fail(exception)))
          } else {
            //            println("got success callback - $message")
            IOs.run(serverAck.complete(metadata))
          }
        }
      }))
    } yield serverAck
  }

//  case class ChunkResult()

  case class BrokerAck(recordMetadata: Seq[RecordMetadata < Aborts[Throwable]] < Fibers)

  def publishChunk[K, V](chunk: IndexedSeq[ProducerRecord[K, V]], producer: KafkaProducer[K, V]): BrokerAck < IOs = {
    for {
      serverAck <- Fibers.initPromise[Seq[RecordMetadata < Aborts[Throwable]]]

      _ <- IOs {
        val length = chunk.size
        try {
          val res = new Array[RecordMetadata < Aborts[Exception]](length)
          val count = new JAtomicLong(0)
          var index = 0
          chunk.foreach { record =>
            val resultIndex = index
            index = index + 1
            producer.send(record, (metadata: RecordMetadata, err: Exception) =>
              val result = if (err != null) {
                Aborts.fail(err)
              } else {
                metadata
              }
              res(resultIndex) = result

              if (count.incrementAndGet == length) {
                IOs.run(serverAck.complete(res))
              }
            )

            ()
          }
        } catch {
          case NonFatal(err) =>
            IOs.run(serverAck.complete(List.fill(length)(Aborts.fail(err))))
        }
      }
    } yield BrokerAck(serverAck.get)
  }

  def publishNoAck(kP: KafkaProducer[String, String], message: String): Unit < IOs = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
    //    println(s"sending $message")
    for {
      //      serverAck <- Fibers.initPromise[Try[RecordMetadata]]
      - <- IOs(kP.send(producerRecord, new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          // TODO don't use Try, but use kyo attempt
          ()
        }
      }))
    } yield ()
  }

//  def publishAll(kp: KafkaProducer[String, String]): Unit < (Envs[KyoProducerConfig] & IOs & Fibers) = {
//    val range: Range.Inclusive = (1 to NumMessages)
//    val program = for {
//      _ <- Streams.initChunk(Chunks.initSeq(range))
//        .transform(i => publish(kp, s"kyo - $i"))
//        .buffer(2048) // Note at this point the messages are sent, and we are just waiting on promises to complete
//        .transform {serverAck =>
//          val foo: String = serverAck.get
//          foo
//        }
//        .runDiscard
//    } yield ()
//
//    program
//  }

  def publishAll3(kp: KafkaProducer[String, String]): Seq[_] < (Envs[KyoProducerConfig] & IOs & Fibers) = {
    // TODO, note this doesn't wait for completion
    val seed: Int < Choices = Choices.get((1 to NumMessages))

    val result: Promise[_] < (Choices & IOs) = seed.map(i => publishWithAttempt(kp, s"kyo - $i"))

    Choices.run(result)
  }

  def publishAll4(kp: KafkaProducer[String, String]): Unit < Fibers = {
    val chunkSize = 10000
//    import Flat.unsafe.bypass[Promise[RecordMetadata < Aborts[Throwable]]]
    Streams.initSeq(1 to (NumMessages / chunkSize))
      .transform { index =>
        val baseIndex= index * chunkSize
        val producerRecords: IndexedSeq[ProducerRecord[String, String]] = (0 to chunkSize).map { chunkIndex =>
          new ProducerRecord[String, String]("escape.heartbeat", s"kyo ${baseIndex + chunkIndex}")
        }
        val result: BrokerAck < IOs = publishChunk(producerRecords, kp)
        result
      }
      .buffer(2048) // Note at this point the messages are sent, and we are just waiting on promises to complete
      .transform { brokerAck =>
        for {
          chunkRecordOrError <- brokerAck.recordMetadata
          // ignoring errors for now
        } yield ()
      }
      .runDiscard
  }

  def main(args: Array[String]): Unit = {
    //    val program: Unit < (IOs & Fibers) = publishAll())
    def timedProgram(producer: KafkaProducer[String, String]) = for {
      _ <- Consoles.println("starting kafka publishing")
      start <- Clocks.now
      _ <- publishAll4(producer)
      end <- Clocks.now
      _ <- Consoles.println(s"Took ${end.toEpochMilli() - start.toEpochMilli()}ms to publish $NumMessages messages")
    } yield ()

    val programLoop = for {
      producer <- producerBuilder
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
    } yield ()
    KyoApp.run(Envs.run(config)(programLoop))
  }
}
