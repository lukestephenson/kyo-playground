package playground

import kyo.*
import kyo.AllowUnsafe.embrace.danger

object ListAndError {
  def main(args: Array[String]): Unit = {
    val list = (1 to 3).toList
    val program: Int < (Choice & IO & Abort[String]) = {
      Choice.eval(list) { i =>
        for {
          _ <- Console.println(s"got $i")
          _ <- if (i == 2) Abort.fail("unexpected 2") else Abort.get(Right(()))
          _ <- Console.println(s"after $i")
        } yield i * 2
      }
    }

    println("Eliminate Abort first")
    {
      val noErrors: Unit < (Choice & IO) = Abort.run[String](program).flatMap{ (result: Result[String, Int]) =>
        Console.println(s"Abort result is $result") }

      val choiceResult = Choice.run(noErrors)

      println(KyoApp.Unsafe.run(choiceResult))
    }
    println()
    println()
    println()
    println()
    println("Eliminate Choice first")
    {
      val choiceResult: Seq[Int] < (IO & Abort[String]) = Choice.run(program)

      val noErrors: Unit < IO = Abort.run[String](choiceResult).flatMap { (result: Result[String, Seq[Int]]) =>
        Console.println(s"Abort result is $result") }

      println(KyoApp.Unsafe.run(noErrors))
    }
    ()
  }
}
