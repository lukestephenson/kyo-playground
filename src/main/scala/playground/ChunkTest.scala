package playground

import kyo.*

object ChunkTest {

  def main(args: Array[String]): Unit = {
    val rangeChunk: Chunk.Indexed[Int] = Chunk.from((1 to 10))
    val myChunk: Seq[Int] = rangeChunk.map(_ * 2)

//    val myChunk2: Seq[Int] = myChunk.map(_ * 2)
    val length = myChunk.size
//    val indexed = myChunk.toIndexed
    println(length)
  }
}
