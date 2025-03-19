package playground

import kyo.*

object Error {

  private def sendEmail(email: String, body: String): Unit < IO = println(s"hello $email - $body")

  private val maybeEmail: Maybe[String] = Present("luke@gmail.com")

  val result:  Unit < IO = maybeEmail match{
    case Present(email) =>
      val body = s"Sent to $email"
      sendEmail(email, body)
    case Absent => ()
  }
  
  def program: Int < (IO & Abort[String]) = {
    for {
      _ <- Console.println("start")
      value <- if (true) Abort.get(Right(5)) else Abort.fail("failed!")
      _ <- Console.println("end")
    } yield value
  }

  def main(args: Array[String]): Unit = {
    val progVal = program
    val handleErrors: Result[String, Int] < IO = Abort.run[String](progVal)
    import AllowUnsafe.embrace.danger
    val result: Result[String, Int] = KyoApp.Unsafe.run(handleErrors)

    // outside kyo app
    println(result)
  }
}
