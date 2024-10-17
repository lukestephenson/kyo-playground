//package playground
//
//import kyo.*
//import kyo.Emit.Ack
//import kyo.Emit.Ack.*
//import kyo.kernel.Effect
//
//
//object StreamBuffer extends KyoApp {
//
//  def buildStream(length: Int): Stream[Int, Any] = {
//    if (length == 0) Stream.init(Seq.empty)
//    else Stream.init(Seq(length)).concat(buildStream(length - 1))
//  }
//
//  val source = buildStream(5)
//  val sourceStream: Stream[Int, Async] = source.map { i =>
//    Console.println(s"pre processing $i").map(_ => i)
//  }
//
//  private val stream = buffer(sourceStream).map{ i =>
//    val result: Int < (IO & Async) = if (i == 3) Async.sleep(10.seconds).map(_ => i)
//    else Console.println(s"post processing $i").map(_ => i)
//
//    result
//  }
//
//
//  // def init[V, S](seq: Seq[V] < S)(using tag: Tag[Emit[Chunk[V]]], frame: Frame): Stream[V, S] =
//
//  private def buffer[V,S](stream: Stream[V,S])(using
//        tagV: Tag[Emit[Chunk[V]]],
//        channelTagV: Tag[Emit[Chunk[Channel[Chunk[V]]]]]): Stream[V, IO & S] = {
//    val bufferChannel: Channel[Chunk[V]] < IO = Channel.init[Chunk[V]](5)
//    val channelSeq: List[Channel[Chunk[V]]] < IO = bufferChannel.map(channel => List(channel))
//
//    val channelStream = Stream.init(channelSeq)
//
//    channelStream.flatMap { channel =>
//      Stream[V, S & Async](Effect.handle.state(tagV, (), stream.v)(
//            [C] =>
//                (input, _, cont) => {
//                  println(s"input is $input")
//
//                  val addToChannelAndEmit: (Unit, kyo.Emit.Ack < (kyo.Emit[kyo.Chunk[V]] & S)) < (kyo.Async & kyo.Emit[kyo.Chunk[V]]) = channel.put(input).map {_ =>
//                    Emit.andMap(Chunk.empty[V])(ack => ((), cont(ack)))
//                  }
//                  
//                  addToChannelAndEmit
//                  // val result: (Unit, kyo.Emit.Ack < (kyo.Emit[kyo.Chunk[V]] & S)) < kyo.Emit[kyo.Chunk[V]] = if input.isEmpty then
//                  //     Emit.andMap(Chunk.empty[V])(ack => ((), cont(ack)))
//                  // else
//                  //     Emit.andMap(Chunk.from(input))(ack => ((), cont(ack)))
//
//                  // result
//                }
//        ))
//    }
//  }
//
//  private def buffer3[V,S](stream: Stream[V,S])(using
//        tagV: Tag[Emit[Chunk[V]]]): Stream[V,S] = {
//    Stream[V, S](Effect.handle.state(tagV, (), stream.v)(
//            [C] =>
//                (input, _, cont) =>
//                  println(s"input is $input")
//                  if input.isEmpty then
//                      Emit.andMap(Chunk.empty[V])(ack => ((), cont(ack)))
//                  else
//                      Emit.andMap(Chunk.from(input))(ack => ((), cont(ack))))
//        )
//  }
//
//  private def buffer2[V,S](stream: Stream[V,S]): Stream[V,S] = {
//    val x = stream.v.map {
//        case Stop => 
//            println("buffer - stop")
//            stream.v
//        case Continue(n) => 
//            println(s"buffer - $n")
//            stream.v
//    }
//    Stream(x)
//  }
//
//  run {
//    for {
//      total <- stream.runFold(0)(_ + _)
//      _ <- Console.println(s"calculated $total")
//    } yield ()
//  }
//}
