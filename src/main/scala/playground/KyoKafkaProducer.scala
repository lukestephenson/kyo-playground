package playground

import kyo.*
import lukekafka.producer.{BrokerAck, Producer}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, RecordMetadata}

case class KyoProducerConfig(config: Map[String, String])

object KyoProducerConfig {
  // TODO sensible producer defaults
  val Defaults = KyoProducerConfig(Map(
  ))

}

object KyoKafkaProducer {
  case object Done
  val chunkSize = 1 // 10000
  val NumMessages = 10 // 10_000_000
  val config = KyoProducerConfig(Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  ))

  val producerBuilder: Producer < (Env[KyoProducerConfig] & Resource & IO) = for {
    _ <- Console.println("instantiating kafka producer")
    producerConfig <- Env.get[KyoProducerConfig]
    errorOrProducer <- Producer.makeDieInvalidConfig(producerConfig.config)
  } yield errorOrProducer

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

//  case class BrokerAck(recordMetadata: Chunk.Indexed[RecordMetadata] < (Async & Abort[Throwable]))
//  case class BrokerAckWithFailures(recordMetadata: Chunk.Indexed[Result[Throwable, RecordMetadata]] < Async)
//
//  def produceChunkAsyncWithFailures[K, V](chunk: Chunk[ProducerRecord[K, V]], producer: KafkaProducer[K, V]): BrokerAckWithFailures < IO = {
//    for {
//      serverAck <- Promise.init[Nothing, Chunk.Indexed[Result[Throwable, RecordMetadata]]]
//
//      _ <- IO {
//        val length = chunk.size
//        try {
//          val res = new Array[Result[Throwable, RecordMetadata]](length)
//          val count = new JAtomicLong(0)
//          var index = 0
//          chunk.foreach { record =>
//            val resultIndex = index
//            index = index + 1
//            producer.send(record, (metadata: RecordMetadata, err: Exception) =>
//              val result = if (err != null) {
//                Result.Fail(err)
//              } else {
//                Result.Success(metadata)
//              }
//              res(resultIndex) = result
//
//              if (count.incrementAndGet == length) {
//                IO.run(serverAck.complete(Result.Success(Chunk.from(res))))
//              }
//            )
//
//            ()
//          }
//        } catch {
//          case NonFatal(err) =>
//            IO.run(serverAck.complete(Result.Success(Chunk.fill(length)(Result.fail(err)).toIndexed)))
//        }
//      }
//    } yield BrokerAckWithFailures(serverAck.get)
//  }
//
//  def produceChunkAsync[K, V](chunk: Chunk[ProducerRecord[K, V]], producer: KafkaProducer[K, V]): BrokerAck < IO = {
//    produceChunkAsyncWithFailures(chunk, producer).map { brokerAck =>
//      val errorOrResults = brokerAck.recordMetadata.map { resultChunk =>
//        val empty: Result[Throwable, Chunk[RecordMetadata]] = Result.Success(Chunk.empty)
//
//        // TODO inefficient
//        resultChunk.foldLeft(empty) { (acc, result) =>
//          acc.flatMap { accChunk =>
//            result.map { metadata =>
//              accChunk.append(metadata)
//            }
//          }
//        } match {
//          case Result.Fail(err) => Abort.fail(err)
//          case Result.Success(chunk) => chunk.toIndexed
//        }
//      }
//      BrokerAck(errorOrResults)
//    }
//  }
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

  def publishAll4(kp: Producer): Done.type < (Async & Abort[Throwable]) = {
    println("publishAll4.a")
    val step1: Stream[BrokerAck, IO] = Stream.init(1 to (NumMessages / chunkSize))
      .map { index =>
        println(s"publishAll4.b - $index")
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

    val step3 = step2.runForeach(x => Console.println(s"publishAll4.c - $x")).map(_ => Done)

    step3
  }

  def main(args: Array[String]): Unit = {
    //    val program: Unit < (IO & Fibers) = publishAll())
    def timedProgram(producer: Producer) = for {
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
