/**
 * This is the main source file for the utility.
 * Created by richard Todd on 10/9/2016.
 */

package rwt.bascat

import java.io.BufferedInputStream
import java.io.FileInputStream

fun main(args: Array<String>) {
    if (args.size < 1) {
        System.err.println("Usage: bascat filename")
        return
    }

    FileInputStream(args[0]).use {
        val bin = BufferedInputStream(it)
        val rdr = fun():Int = bin.read()
        val readFunc = when(bin.read()) {
            0xFF ->  rdr
            0xFE ->  { val unp = Unprotector(rdr) ;  fun():Int = unp.getAsInt() }
            else ->  throw IllegalArgumentException("Not a BAS bytecode file")
        }

        BasCat( BinaryReader(readFunc) ).cat()
    }

}

class BasCat(private val rdr: BinaryReader) {

    private fun nextToken() : Token {
        val nxt = rdr.readu8()
        return when(nxt) {
            in 0x20..0x7E -> Token.fromLiteral(nxt.toChar())
            in 0x11..0x1B -> Token.fromNumber(nxt.toLong() - 0x11, 10)
            in 0xFD..0xFF -> Token.fromOpcode( (nxt shl 8) or rdr.readu8() )
            -1 -> throw Exception("Unexpected EOF!")
            0x0E -> Token.fromNumber(rdr.readu16().toLong(), 10)
            0x0B -> Token.fromNumber(rdr.read16().toLong(),8)
            0x0C -> Token.fromNumber(rdr.read16().toLong(), 16)
            0x1C -> Token.fromNumber(rdr.read16().toLong(), 10)
            0x0F -> Token.fromNumber(rdr.readu8().toLong(), 10)
            0x1D -> Token.fromFloat(rdr.readf32())
            0x1F -> Token.fromFloat(rdr.readf64())
            else -> Token.fromOpcode(nxt)
        }
    }

    private fun line() : Sequence<Token>? {
        if (rdr.readu16() == 0) {
            return null
        }

        return sequenceOf(Token.fromNumber(rdr.readu16().toLong(), 10), Token.fromLiteral("  ")) +
                 (generateSequence { nextToken() }.takeWhile { it.opcode != 0 })
    }

    fun cat() : Unit {
        generateSequence { line() }.forEach { printLine(it) }
    }

    private fun  printLine(toks: Sequence<Token>) : Unit {
        // it would be nice to come up with a way to avoid converting the sequence
        // into a list, but Kotlin doesn't have a sequence 'match' like Scala does.
        // Maybe there's a better way, but with my current level of Kotlin-fu, I just
        // converted it to a list and moved on.
        val lst = toks.toList()
        var idx = 0
        val sz = lst.size
        while (idx < sz) {
            var cur = lst[idx]
            // there are a couple cases where we need to peek ahead and possibly output an alternate token...
            if (cur.opcode == 0x3A) {
                if (idx + 1 < sz && lst[idx + 1].opcode == 0xA1) {
                    cur = lst[++idx]
                } else if (idx + 2 < sz &&
                        lst[idx + 1].opcode == 0x8F &&
                        lst[idx + 2].opcode == 0xD9) {
                    idx += 2
                    cur = lst[idx]
                }
            } else if (cur.opcode == 0xB1 &&
                    idx + 1 < sz &&
                    lst[idx + 1].opcode == 0xE9) {
                idx++ /* skip the next one */
            }

            print(cur.description)
            idx++
        }
        println("")
    }
}
