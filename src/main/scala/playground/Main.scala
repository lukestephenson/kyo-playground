//package playground
//
//import scala.concurrent.duration.*
//import kyo._
//
//trait MyService {
//  def hello(name: String): Unit < Consoles
//}
//
//trait Subscriber[A] {
//  def onNext(e: A): Unit
//}
//
//trait Observable[A] {
//  def subscribe(subscriber: Subscriber[A]): Unit < Fibers
//
//  def complete: Unit = {
//    this.subscribe(new Subscriber[A] {
//      def onNext(e: A): Unit = ()
//    })
//  }
////  def map[B, S2](f: A => B < S2): Observable[B]
//}
//
//object Observable {
//  def range(start: Int, end: Int): Observable[Int] < IOs = new Observable[Int] {
//    override def subscribe(subscriber: Subscriber[Int]): Unit < Fibers = {
//      def emit(channel: Channel[Int], next:Int): Unit < Fibers = {
//        if (next <= end) channel.put(next).map(_ => emit(channel, next + 1))
//        else ()
//      }
//      def take(channel: Channel[Int]): Unit < Fibers = {
//        channel.take.map(subscriber.onNext)
//      }
//      Channels.init[Int](1).map { channel =>
//        for {
//          _ <- emit(channel, start)
//          _ <- take(channel)
//        } yield ()
//      }
//    }
//  }
//}
//
//object Main extends KyoApp {
//  run {
//    val nameVal = maybeName
//    val noName = Options(null)
//    val channel: Channel[String] < IOs = Channels.init[String](10)
//    val program = for {
//      name <- maybeName
//      _ <- Consoles.println(s"Name is $name")
//      service <- Envs.get[MyService]
//      _ <- service.hello(name)
//      _ <- Consoles.println(s"Add to channel")
//      liveChannel <- channel
//      _ <- liveChannel.put(name)
//      _ <- Consoles.println(s"Take from channel")
//      taken <- liveChannel.take
//      _ <- Consoles.println(s"from a channel took $taken")
//    } yield ()
//
//    val stream = Streams.initSeq((0 to 10))
//
//    def transformFn(i: Int): Int < Fibers = {
//      val result = if (i == 5)
//        Fibers.delay(5.seconds)(i*2)
//      else
//        i * 2
//
//      result
//    }
//
//    val streamResult: (Long, Unit) < (Fibers & Consoles) = for {
//      start <- Clocks.now
//      tuple <- stream.transform(transformFn).runFold(0L)(_+_)
//      end <- Clocks.now
//      _ <- Consoles.println(s"Took ${end.toEpochMilli()-start.toEpochMilli()}ms to calculate $tuple")
//    } yield tuple
//
////    val program2: Unit < (Options & IOs & Envs[MyService]) = maybeName.flatMap { name =>
////      for {
////        _ <- Consoles.println(s"Name is $name")
////        service <- Envs[MyService].get
////        _ <- service.hello(name)
////      } yield ()
////    }
////    for {
////      _ <- maybeName.flatMap(name => Consoles.println(s"Name is ${maybeName}"))
////      _ <- Consoles.println(s"Name is ${maybeName}")
////    } yield ()
//    val exampleService = new MyService {
//      override def hello(name: String): Unit < Consoles = Consoles.println(s"Hello $name $streamResult")
//    }
////    val p2 = Envs[MyService].run(exampleService)(Options.run(program))
////    p2
//
//     for {
//       _ <- streamResult
//       _ <- streamResult
//       _ <- streamResult
//       _ <- streamResult
//     } yield ()
//  }
//  def maybeName: String < Options = Options("Luke")
//}
