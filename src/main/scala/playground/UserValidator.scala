package playground

import kyo.*

case class User(email: String, name: String, age: Int)

object UserValidator {

  enum UserValidationError {
    case InvalidAge, InvalidEmail
  }

  def parse(email: String, name: String, age: Int): Result[UserValidationError, User] = {
    val kyoProgram: User < Abort[UserValidationError] = parseKyo(email, name, age)

    Abort.run(kyoProgram).eval
  }

  def parseKyo(email: String, name: String, age: Int): User < Abort[UserValidationError] = {
    for {
      validEmail <- validateEmail(email)
      validAge <- validateAge(age)
    } yield User(validEmail, name, validAge)
  }

  def validateEmail(email: String): String < Abort[UserValidationError] = {
    if (email.contains("@")) email else Abort.fail(UserValidationError.InvalidEmail)
  }

  def validateAge(age: Int): Int < Abort[UserValidationError] = {
    if (age > 0 & age < 200) age
    else Abort.fail(UserValidationError.InvalidAge)
  }

  def main(args: Array[String]): Unit = {
    println(parse("luke@gmail.com", "luke", -35))
  }
}
