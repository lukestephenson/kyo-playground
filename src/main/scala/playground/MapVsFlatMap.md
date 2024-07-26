```scala
sealed trait MyOption[A]
case class MySome[A](value: A) extends MyOption[A] {
  def map[B](f: A => B): MyOption[B] = flatMap(a => MySome(f(value)))

  def flatMap[B](f: A => MyOption[B]): MyOption[B] = f(value)
}
```