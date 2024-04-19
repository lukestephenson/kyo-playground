package playground

import kyo.{<, Clocks, Consoles, Envs, Fiber, Fibers, IOs, KyoApp, Promise, Streams, core, flatMap, map}
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.*
import scala.jdk.javaapi.FutureConverters

case class KyoProducerConfig(config: Map[String, Object])

object KyoKafkaProducer {
  val NumMessages = 10 // 1_000_000
  val config = KyoProducerConfig(Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  ))

  val producer: KafkaProducer[String, String] < (Envs[KyoProducerConfig] & IOs) = for {
    producerConfig <- Envs[KyoProducerConfig].get
    javaProps = producerConfig.config.asJava
    p <- IOs(new KafkaProducer[String, String](javaProps, new StringSerializer(), new StringSerializer()))
  } yield p

  /**
   * Will publish until blocked on the client side
   *
   * Return a promise
   */
  def publish(kP: KafkaProducer[String, String], message: String): Fiber[Try[RecordMetadata]] < IOs = {
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
        }}))
      serverAck <- Fibers.fromFutureFiber(promise.future)
    } yield serverAck
  }

  def publishFirstAttempt(kP: KafkaProducer[String, String], message: String): Promise[Try[RecordMetadata]] < IOs = {
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

  def publishAll(): Unit < (Envs[KyoProducerConfig] & IOs & Fibers) = {
    val program = for {
      kp <- producer
      _ <- Streams.initSeq(1 to NumMessages)
        .transform(i => publish(kp, s"kyo - $i"))
        .buffer(20000) // Note at this point the messages are sent, and we are just waiting on promises to complete
        .transform(serverAck => serverAck.get)
        .runDiscard
    } yield ()

    program
  }

  def main(args: Array[String]): Unit = {
    val program: Unit < (IOs & Fibers) = Envs[KyoProducerConfig].run(config)(publishAll())
    val timedProgram = for {
      _ <- Consoles.println("starting kafka publishing")
      start <- Clocks.now
      _ <- program
      end <- Clocks.now
      _ <- Consoles.println(s"Took ${end.toEpochMilli()-start.toEpochMilli()}ms to publish $NumMessages messages")
    } yield ()
    KyoApp.run(timedProgram)
  }
}
