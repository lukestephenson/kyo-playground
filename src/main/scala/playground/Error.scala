package playground

import kyo.*
import kyo.IOs

object Error {

  val a: Int = 1
  val b: Int < Any = 1
  val b1: Int = 1.pure
  val c: Int < Options = 1
  val c1: Option[Int] < Any = Options.run(c)
  val c2: Option[Int] = c1.pure

  def sendEmail(email: String, body: String): Unit < IOs = println(s"hello $email")

  val maybeEmail: String < Options = "luke@gmail.com"

  val result1: Unit < (Options & IOs) = maybeEmail.map { email =>
    val body = s"Sent to $email"
    sendEmail(email, body)
  }

  val result2: Unit < (Options & IOs) = maybeEmail.flatMap { email =>
    val body = s"Sent to $email"
    sendEmail(email, body)
  }


  for {
    email <- maybeEmail
    body = s"Sent to $email"
    _ <- sendEmail(email, body)
  } yield ()



  def program: Int < (Consoles & Aborts[String]) = {
    for {
      _ <- Consoles.println("start")
      value <- if (true) 5 else Aborts.fail("failed!")
      _ <- Consoles.println("end")
    } yield value
  }

  def main(args: Array[String]): Unit = {
    val progVal = program
    val handleErrors: Either[String, Int] < kyo.Consoles = Aborts.run[String](progVal)
    val result: Either[String, Int] = KyoApp.run(handleErrors)

    // outside kyo app
    println(result)
  }
}
