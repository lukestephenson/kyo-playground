package playground

import kyo.*

object Traverse {

  def traverse[A, B: Flat, S](list: List[A])(f: A => B < S): List[B] < S = {
    val foo: B < (Choices & S) = Choices.eval(list)(f)
    
    val seq: Seq[B] < S = Choices.run(foo)
    
    seq.flatMap(a => a.toList)
  }

  def traverse2[A, B: Flat, S](list: List[A])(f: A => B < S): List[B] < S = {
    val foo: Chunk[A] = Chunks.initSeq(list)

    val chunk: Chunk[B] < S = foo.map(f)

    chunk.flatMap(_.toSeq.toList)
  }

  def logAndDouble(i: Int): Int < Consoles = {
    for {
      _ <- Consoles.println(s"Prog - hello $i")
    } yield i * 2
  }
  
  def main(args: Array[String]): Unit = {
    val myList = List(1,2)

    val prog1: List[Int] < Consoles = traverse(myList)(logAndDouble)

    val prog2: List[Int] < Consoles = traverse2(myList)(logAndDouble)

    println("Prog1")
    println(KyoApp.run(prog1))
    println("Prog2")
    println(KyoApp.run(prog2))
  }
}
