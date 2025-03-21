package playground

import kyo.*
import kyo.KyoApp

import java.io.IOException

case class InvalidName(name:String)

object AbortExample extends KyoApp {
  def validateAge: Int < Abort[String] = {
    123 // Abort.fail("Luke failed!")
  }

  def validateName: String < Abort[InvalidName] = {
    Abort.fail(InvalidName("invalid name"))
  }
  run {
    val prog: (Int, String) < (Abort[String | InvalidName] & IO & Abort[IOException]) = for {
      _ <- Console.printLine("start")
      age <- validateAge
      name <- validateName
      _ <- Console.printLine("end")
    } yield (age, name)

    val handleErrors: (Int, String) < (Abort[String] & IO & Abort[IOException]) = Abort.recover[InvalidName] {
      case InvalidName(name) => Console.printLine(s"Invalid name: $name").map(_ => (0, "invalid"))
    }(prog)

    Abort.run(handleErrors)
  }
}
