package playground

import kyo.*

object ListAndError {
  def main(args: Array[String]): Unit = {
    val list = (1 to 3).toList
    val program: Int < (Choices & Consoles & Aborts[String]) = {
      Choices.eval(list) { i =>
        for {
          _ <- Consoles.println(s"got $i")
          _ <- if (i == 2) Aborts.fail("unexpected 2") else ()
          _ <- Consoles.println(s"after $i")
        } yield i * 2
      }
    }

    println("Eliminate Aborts first")
    {
      val noErrors: Unit < (Choices & Consoles) = Aborts.run[String](program).flatMap{ (result: Either[String, Int]) =>
        Consoles.println(s"Aborts result is $result") }

      val choiceResult = Choices.run(noErrors)

      println(KyoApp.run(choiceResult))
    }
    println()
    println()
    println()
    println()
    println("Eliminate Choices first")
    {
      val choiceResult: Seq[Int] < (Consoles & Aborts[String]) = Choices.run(program)

      val noErrors: Unit < Consoles = Aborts.run[String](choiceResult).flatMap { (result: Either[String, Seq[Int]]) =>
        Consoles.println(s"Aborts result is $result") }

      println(KyoApp.run(noErrors))
    }
    ()
  }
}
