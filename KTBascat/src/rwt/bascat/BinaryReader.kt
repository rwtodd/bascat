package rwt.bascat

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A class that knows how to pull binary data out of a
 * tokenized BAS file.
 * Created by richard todd on 10/9/2016.
 */
class BinaryReader(private val src: () -> Int) {
    private val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)

    fun read8() : Int = src()

    fun readu8() = read8() and 0xFF

    fun read16() : Int {
        buf.clear()
        buf.put(src().toByte()).put(src().toByte()).flip()
        return buf.getShort().toInt()
    }

    fun readu16() = read16() and 0xFFFF

    fun readf32() : Double {
        buf.clear()
        buf.put(src().toByte())
            .put(src().toByte())
            .put(src().toByte())
            .put(src().toByte())
            .flip()
        val bs = buf.array()

        if(bs[3] != 0.toByte()) {
            val sign = bs[2].toInt() and 0x80
            val exp = (bs[3] - 2) and 0xFF
            bs[3] = (sign or (exp ushr 1)).toByte()
            bs[2] = ((exp shl 7) or (bs[2].toInt() and 0x7F)).toByte()
            return buf.getFloat().toDouble()
        }
        return 0.0;
    }

     fun readf64() : Double {
         val bs = byteArrayOf(
                 src().toByte(),
                 src().toByte(),
                 src().toByte(),
                 src().toByte(),
                 src().toByte(),
                 src().toByte(),
                 src().toByte(),
                 src().toByte()
         )

         // convert MBF to IEEE
         //  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee

         if (bs[7].toInt() != 0) {
             val iees = ByteArray(8)

             val sign = bs[6].toInt() and 0x80
             val exp = (bs[3].toInt() - 128 - 1 + 1023) and 0xFFFF

             iees[7] = (sign or (exp ushr 4)).toByte()
             iees[6] = (exp shl 4).toByte()

             for (idx in 6 downTo 1) {
                 bs[idx] = ((bs[idx].toInt() shl 1) or (bs[idx - 1].toInt() ushr 7) and 0xFF).toByte()
                 iees[idx] = (iees[idx].toInt() or (bs[idx].toInt() ushr 4)).toByte()
                 iees[idx - 1] = (iees[idx - 1].toInt() or (bs[idx].toInt() shl 4)).toByte()
             }
             bs[0] = (bs[0].toInt() shl 1).toByte()
             iees[0] = (iees[0].toInt() or (bs[0].toInt() ushr 4)).toByte()

             return ByteBuffer.wrap(iees).order(ByteOrder.LITTLE_ENDIAN).double
         }
         return 0.0
     }
}