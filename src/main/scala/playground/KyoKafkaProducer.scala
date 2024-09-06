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

  val producerBuilder: KafkaProducer[String, String] < (Env[KyoProducerConfig] & IO) = for {
    _ <- Console.println("instantiating kafka producer")
    producerConfig <- Env.get[KyoProducerConfig]
    javaProps = producerConfig.config.asJava
    p <- IO(new KafkaProducer[String, String](javaProps, new StringSerializer(), new StringSerializer()))
  } yield p

  // /**
  //  * Will publish until blocked on the client side
  //  *
  //  * Return a promise
  //  */
  // def publishSecondAttempt(kP: KafkaProducer[String, String], message: String): Fiber[Throwable, RecordMetadata] < IO = {
  //   val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
  //   //    println(s"sending $message")

  //   for {
  //     promise <- IO(scala.concurrent.Promise[Try[RecordMetadata]]())
  //     _ <- IO(kP.send(producerRecord, new Callback {
  //       override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
  //         // TODO don't use Try, but use kyo attempt for lower overhead
  //         if (exception != null) {
  //           //            println(s"got failure callback - $message")
  //           promise.success(Failure(exception))
  //         } else {
  //           //                        println(s"got success callback - $message")
  //           promise.success(Success(metadata))
  //         }
  //       }
  //     }))
  //     serverAck <- Async.fromFutureFiber(promise.future)
  //   } yield serverAck
  // }

  // def publish(kP: KafkaProducer[String, String], message: String): Promise[Try[RecordMetadata]] < IO = {
  //   val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
  //   //    println(s"sending $message")
  //   for {
  //     serverAck <- Fibers.initPromise[Try[RecordMetadata]]
  //     - <- IO(kP.send(producerRecord, new Callback {
  //       override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
  //         // TODO don't use Try, but use kyo attempt for lower overhead
  //         if (exception != null) {
  //           IO.runLazy(serverAck.complete(Failure(exception)))
  //         } else {
  //           //            println("got success callback - $message")
  //           IO.runLazy(serverAck.complete(Success(metadata)))
  //         }
  //       }
  //     }))
  //   } yield serverAck
  // }

  // def publishWithAttempt(kP: KafkaProducer[String, String], message: String): Promise[RecordMetadata < Abort[Exception]] < IO = {
  //   val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
  //   //    println(s"sending $message")
  //   for {
  //     serverAck <- Fibers.initPromise[RecordMetadata < Abort[Exception]]
  //     - <- IO(kP.send(producerRecord, new Callback {
  //       override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
  //         // TODO don't use Try, but use kyo attempt for lower overhead
  //         if (exception != null) {
  //           IO.run(serverAck.complete(Abort.fail(exception)))
  //         } else {
  //           //            println("got success callback - $message")
  //           IO.run(serverAck.complete(metadata))
  //         }
  //       }
  //     }))
  //   } yield serverAck
  // }

//  case class ChunkResult()

  case class BrokerAck(recordMetadata: Chunk[Result[Throwable, RecordMetadata]] < Async)

  def publishChunk[K, V](chunk: Chunk[ProducerRecord[K, V]], producer: KafkaProducer[K, V]): BrokerAck < IO = {
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
                IO.run(serverAck.complete(Result.Success(Chunk.from(res))))
              }
            )

            ()
          }
        } catch {
          case NonFatal(err) =>
            IO.run(serverAck.complete(Result.Success(Chunk.fill(length)(Result.fail(err)).toIndexed)))
        }
      }
    } yield BrokerAck(serverAck.get)
  }

  // def publishNoAck(kP: KafkaProducer[String, String], message: String): Unit < IO = {
  //   val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
  //   //    println(s"sending $message")
  //   for {
  //     //      serverAck <- Fibers.initPromise[Try[RecordMetadata]]
  //     - <- IO(kP.send(producerRecord, new Callback {
  //       override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
  //         // TODO don't use Try, but use kyo attempt
  //         ()
  //       }
  //     }))
  //   } yield ()
  // }

//  def publishAll(kp: KafkaProducer[String, String]): Unit < (Envs[KyoProducerConfig] & IO & Fibers) = {
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

  // def publishAll3(kp: KafkaProducer[String, String]): Seq[_] < (Envs[KyoProducerConfig] & IO & Fibers) = {
  //   // TODO, note this doesn't wait for completion
  //   val seed: Int < Choice = Choice.get((1 to NumMessages))

  //   val result: Promise[_] < (Choice & IO) = seed.map(i => publishWithAttempt(kp, s"kyo - $i"))

  //   Choice.run(result)
  // }

  def publishAll4(kp: KafkaProducer[String, String]): Unit < IO = {
    val chunkSize = 10000
//    import Flat.unsafe.bypass[Promise[RecordMetadata < Abort[Throwable]]]
    Stream.init(1 to (NumMessages / chunkSize))
      .map { index =>
        val baseIndex= index * chunkSize
        val producerRecords: IndexedSeq[ProducerRecord[String, String]] = (0 to chunkSize).map { chunkIndex =>
          new ProducerRecord[String, String]("escape.heartbeat", s"kyo ${baseIndex + chunkIndex}")
        }
        val result: BrokerAck < IO = publishChunk(Chunk.from(producerRecords), kp)
        result
      }
      // .buffer(2048) // Note at this point the messages are sent, and we are just waiting on promises to complete
      .map { brokerAck =>
        for {
          chunkRecordOrError <- brokerAck.recordMetadata
          // ignoring errors for now
        } yield ()
      }
      .runDiscard
  }

  def main(args: Array[String]): Unit = {
    //    val program: Unit < (IO & Fibers) = publishAll())
    def timedProgram(producer: KafkaProducer[String, String]) = for {
      _ <- Console.println("starting kafka publishing")
      start <- Clock.now
      _ <- publishAll4(producer)
      end <- Clock.now
      _ <- Console.println(s"Took ${end.toEpochMilli() - start.toEpochMilli()}ms to publish $NumMessages messages")
    } yield ()

    val programLoop = for {
      producer <- producerBuilder
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
    } yield ()
    KyoApp.run(Env.run(config)(programLoop))
  }
}
