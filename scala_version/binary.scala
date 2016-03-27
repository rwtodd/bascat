// copyright 2016 Richard Todd.
// License is GPL... see the LICENSE file in the repository.

package rwt.bascat

import java.nio.{ByteBuffer,ByteOrder}

/** Binary contains helper methods to
  * read binary data from a stream.
  */
class BinaryReader(src: ()=>Int) {
  private val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)

  def readu8(): Int = src()

  def read16(): Int =  {
     buf.clear()
     buf.put(src().toByte).put(src().toByte).flip()
     buf.getShort()
  }

  def readu16(): Int =  read16() & 0xFFFF

  def readf32(): Double = {
     buf.clear()
     buf.put(src().toByte).put(src().toByte).
         put(src().toByte).put(src().toByte).
         flip()
     val bs = buf.array

     // convert MBF to IEEE
     //  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee
     if (bs(3) != 0) {
                val sign:Int = bs(2) & 0x80
                val exp:Int = (bs(3) - 2) & 0xFF
                bs(3) = (sign | (exp >> 1)).toByte
                bs(2) = ((exp << 7) | (bs(2) & 0x7F)).toByte
                return buf.getFloat.toDouble
     }
     return 0.0
  }

  def readf64(): Double = {
     val bs = Array(src(),src(),src(),src(),
                    src(),src(),src(),src())

     // convert MBF to IEEE
     //  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee

     if (bs(7) != 0) {
                val iees = new Array[Byte](8)

                val sign: Int = bs(6) & 0x80
                val exp: Int = (bs(3) - 128 - 1 + 1023) & 0xFFFF

                iees(7) = (sign | (exp>>4)).toByte
                iees(6) = (exp << 4).toByte

                for(idx <- 6 to  1 by -1) {
                    bs(idx) = ( (bs(idx) << 1) | (bs(idx-1) >> 7) ) & 0xFF
                    iees(idx) = (iees(idx) | (bs(idx) >> 4)).toByte
                    iees(idx-1) = (iees(idx-1) | (bs(idx) << 4)).toByte
                }
                bs(0) = (bs(0) << 1).toByte
                iees(0) = (iees(0) | (bs(0) >> 4)).toByte

                return ByteBuffer.wrap(iees).order(ByteOrder.LITTLE_ENDIAN).getDouble()
     }
     return 0.0
  } 
}
