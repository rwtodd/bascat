// copyright 2016 Richard Todd.
// License is GPL... see the LICENSE file in the repository.

package rwt.bascat

import java.io.{EOFException,FileInputStream,BufferedInputStream}

final class BasCat(src: ()=>Int) {

  private val in = new BinaryReader(src)

  @annotation.tailrec
  private def printLine(in: List[Token]):Unit = {
    val (str,lst) = in match { 
      case Nil => System.out.println(""); return
      case Token(0x3A,_) :: Token(0xA1,out) :: rest                  => (out,rest)
      case Token(0x3A,_) :: Token(0x8F,_) :: Token(0xD9,out) :: rest => (out,rest)
      case Token(0xB1,out) :: Token(0xE9,_) :: rest                  => (out,rest)
      case Token(_,out) :: rest                                      => (out,rest)
    }

    System.out.print(str)
    printLine(lst)    
  }

  private def nextToken(): Token = in.readu8() match {
      case -1 =>  throw new EOFException("Unexpected EOF!")
      case x if (x >= 0x20 && x <= 0x7E) => Token.fromLiteral(x)           // ASCII literals 
      case x if (x >= 0x11 && x <= 0x1B) => Token.fromNumber(x - 0x11, 10) // the digits 0 to 10
      case 0x0E => Token.fromNumber(in.readu16(), 10)                      // line-number     
      case 0x0B => Token.fromNumber(in.read16(), 8)                        // octal number
      case 0x0C => Token.fromNumber(in.read16(), 16)                       // hex number
      case 0x1C => Token.fromNumber(in.read16(), 10)                       // 2-byte int
      case 0x0F => Token.fromNumber(in.readu8(), 10)                       // 1-byte int
      case 0x1D => Token.fromFloat(in.readf32())                           // 4-byte float 
      case 0x1F => Token.fromFloat(in.readf64())                           // 8-byte float 
      case x if (x >= 0xFD && x <= 0xFF) => 
                                 Token.fromOpcode( (x<<8) | in.readu8() )  // 2-byte opcodes 
      case x => Token.fromOpcode(x)                                        // 1-byte opcodes
     } 

  private def line(): List[Token] = {
        if(in.readu16() == 0) return Nil               // 0 pointer means EOF
        val lineNo = Token.fromNumber(in.readu16(),10) // line number
        val toks = Stream.continually(nextToken).      // line tokens
                          takeWhile(_.op != 0).
                          toList
        lineNo :: Token.fromLiteral("  ") :: toks
  }

  def cat(): Unit = Stream.continually(line).takeWhile(!_.isEmpty).foreach(printLine)

}

object BasCat {

   def main(args: Array[String]): Unit = {
     var fin = new FileInputStream(args(0))
     try {
       val bin = new BufferedInputStream(fin)
       val readFunc = bin.read() match {
         case 0xFF => bin.read _ 
         case 0xFE => new Unprotector(bin).read _
         case _    => throw new Exception("Bad 1st Byte!")
                      return 
       } 
       new BasCat(readFunc).cat()
     } catch {
        case e: Exception => System.err.println(e)
     } finally {
        fin.close()
     }
   }

}
