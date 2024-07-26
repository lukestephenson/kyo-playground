package playground

import kyo.*

case class User(email: String, name: String, age: Int)

object UserValidator {

  enum UserValidationError {
    case InvalidAge, InvalidEmail
  }

  def parse(email: String, name: String, age: Int): Either[UserValidationError, User] = {
    val kyoProgram: User < Aborts[UserValidationError] = parseKyo(email, name, age)

    Aborts.run(kyoProgram).pure
  }

  def parseKyo(email: String, name: String, age: Int): User < Aborts[UserValidationError] = {
    for {
      validEmail <- validateEmail(email)
      validAge <- validateAge(age)
    } yield User(validEmail, name, validAge)
  }

  def validateEmail(email: String): String < Aborts[UserValidationError] = {
    if (email.contains("@")) email else Aborts.fail(UserValidationError.InvalidEmail)
  }

  def validateAge(age: Int): Int < Aborts[UserValidationError] = {
    if (age > 0 & age < 200) age
    else Aborts.fail(UserValidationError.InvalidAge)
  }

  def main(args: Array[String]): Unit = {
    println(parse("luke@gmail.com", "luke", -35))
  }
}
