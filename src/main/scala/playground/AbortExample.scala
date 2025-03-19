package playground

import kyo.*
import kyo.KyoApp

case class InvalidName(name:String)

object AbortExample extends KyoApp {
  def validateAge: Int < Abort[String] = {
    123 // Abort.fail("Luke failed!")
  }

  def validateName: String < Abort[InvalidName] = {
    Abort.fail(InvalidName("invalid name"))
  }
  run {
    val prog: (Int, String) < (Abort[String | InvalidName] & IO) = for {
      _ <- Console.println("start")
      age <- validateAge
      name <- validateName
      _ <- Console.println("end")
    } yield (age, name)

    val handleErrors: (Int, String) < (Abort[String] & IO) = Abort.recover[InvalidName] {
      case InvalidName(name) => Console.println(s"Invalid name: $name").as((0, "invalid"))
    }(prog)

    Abort.run(handleErrors)
  }
}
