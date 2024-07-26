package playground

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.reactive.{Observable, OverflowStrategy}
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Promise
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.*
import scala.jdk.javaapi.FutureConverters
import scala.concurrent.{Promise, blocking}

object ObservableKafkaProducer extends TaskApp {
  val NumMessages = 10_000_000
  val config = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka.docker:9092",
    ProducerConfig.LINGER_MS_CONFIG -> "100",
    ProducerConfig.BATCH_SIZE_CONFIG -> "16384"
  )

  val producerBuilder: Task[KafkaProducer[String, String]] = for {
    _ <- Task(println("instantiating kafka producer"))
    p <- Task(new KafkaProducer[String, String](config.asJava, new StringSerializer(), new StringSerializer()))
  } yield p

  def publish(kP: KafkaProducer[String, String], message: String): Task[Promise[RecordMetadata]] = {
    val producerRecord = new ProducerRecord[String, String]("escape.heartbeat", message)
    //    println(s"sending $message")
    val serverAck = Promise[RecordMetadata]()
    for {
      - <- Task {
        blocking {
          kP.send(producerRecord, new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
              // TODO don't use Try, but use kyo attempt for lower overhead
              if (exception != null) {
                serverAck.complete(Failure(exception))
              } else {
                //            println("got success callback - $message")
                serverAck.complete(Success(metadata))
              }
            }
          }
          )
        }
      }
    } yield serverAck
  }

  def publishAll(kp: KafkaProducer[String, String]) = {
    val program = for {
      _ <- Observable.range(1, NumMessages)
        .mapEval(i => publish(kp, s"kyo - $i"))
        .asyncBoundary(OverflowStrategy.BackPressure(2048)) // Note at this point the messages are sent, and we are just waiting on promises to complete
        .mapEval(serverAck => Task.fromFuture(serverAck.future))
        .completedL
    } yield ()

    program
  }

  override def run(args: List[String]): Task[ExitCode] = {
    //    val program: Unit < (IOs & Fibers) = publishAll())
    def timedProgram(producer: KafkaProducer[String, String]): Task[Unit] = for {
      _ <- Task(println("starting kafka publishing"))
      start <- Task(System.currentTimeMillis())
      _ <- publishAll(producer)
      end <- Task(System.currentTimeMillis())
      _ <- Task(println(s"Took ${end - start}ms to publish $NumMessages messages"))
    } yield ()

    val programLoop = for {
      producer <- producerBuilder
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
      _ <- timedProgram(producer)
    } yield ExitCode.Success

    programLoop
  }
}
