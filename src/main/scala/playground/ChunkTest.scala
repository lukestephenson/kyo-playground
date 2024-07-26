package playground

import kyo.*

object ChunkTest {

  def main(args: Array[String]): Unit = {
    val rangeChunk: Chunks.Indexed[Int] = Chunks.initSeq((1 to 10)).toIndexed
    val myChunk: Chunk[Int] = rangeChunk.map(_ * 2).pure

    val myChunk2: Chunk[Int] = myChunk.map(_ * 2).pure
    val length = myChunk.size
    val indexed = myChunk.toIndexed
    println(length)
  }
}
