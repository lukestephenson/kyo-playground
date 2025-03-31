package kyo

import scala.reflect.ClassTag

object ChunkExtensions {
  // Only because the Kafka producer not currently in the correct package
  def fromArrayNoCopy[A](array: Array[A]): Chunk.Indexed[A] = {
    Chunk.fromNoCopy(array)
  }
  
  extension [A](chunk: Chunk.Indexed[A]) {
    def fastMap[B: ClassTag](f: A => B):Chunk.Indexed[B] = {
      val result = new Array[B](chunk.size)
      var i = 0
      while (i < chunk.size) {
        result(i) = f(chunk(i))
        i += 1
      }
      ChunkExtensions.fromArrayNoCopy(result)
    }
    
  }
}
