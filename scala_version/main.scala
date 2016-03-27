package rwt.bascat

import java.io.{EOFException,FileInputStream,BufferedInputStream}

final class BasCat(src: ()=>Int) {

  private val in = new BinaryReader(src)

  private def skipPointer(): Unit = in.readu16() match { 
    case 0 => throw new EOFException("EOF")
    case _ => 
  }

  private def readLineNo(): Int = in.readu16()
  
  private def nextToken(): Option[Token] = in.readu8() match {
      // ASCII literals 
      case x if (x >= 0x20 && x <= 0x7E) => Some(Token.fromLiteral(x))

      // 11 -> 1B are the digits 0 to 10
      case x if (x >= 0x11 && x <= 0x1B) => Some(Token.fromNumber(x - 0x11, 10))

      // line-number
      case 0x0E =>   Some(Token.fromNumber(in.readu16(), 10))
      
      // octal number
      case 0x0B =>   Some(Token.fromNumber(in.read16(), 8))

      // hex number
      case 0x0C =>   Some(Token.fromNumber(in.read16(), 16))

      // 2-byte int
      case 0x1C =>   Some(Token.fromNumber(in.read16(), 10))

      // 1-byte int
      case 0x0F =>   Some(Token.fromNumber(in.readu8(), 10))

      // 4-byte float 
      case 0x1D =>   Some(Token.fromFloat(in.readf32()))

      // 8-byte float 
      case 0x1F =>   Some(Token.fromFloat(in.readf64()))

      // 2-byte opcodes 
      case x if (x >= 0xFD && x <= 0xFF) =>
          Some(Token.fromOpcode( (x<<8) | in.readu8() ))
 
      // end of line
      case 0x00 => None

      // 1-byte opcodes
      case x => Some(Token.fromOpcode(x))
     } 

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

  @annotation.tailrec
  private def toks(acc:List[Token]): List[Token] = 
      nextToken() match {
        case Some(t) => toks(t :: acc)
        case None    => acc.reverse
      }
   
  private def line(): List[Token] = {
      try {
        skipPointer
        val lineNo = Token.fromNumber(readLineNo(),10)
        lineNo :: Token.fromLiteral("  ") :: toks(Nil)
      } catch {
        case eof: EOFException => Nil
      }
  }

  @annotation.tailrec
  def cat(): Unit = line() match { 
     case Nil => return 
     case ln  => printLine(ln)
                 cat()
  }

}

object BasCat {

   def main(args: Array[String]): Unit = {
     var fin = new FileInputStream(args(0))
     try {
       val bin = new BufferedInputStream(fin)
       val readFunc = bin.read() match {
         case 0xFF => () => { val ch = bin.read() 
                              if (ch == -1) throw new EOFException("EOF")
                              ch 
                            }
         case 0xFE => new Unprotector(bin).read _
         case _    => throw new Exception("Bad 1st Byte!")
                      return 
       } 
       new BasCat(readFunc).cat()
     } catch {
        case e: Exception => e.printStackTrace(); System.err.println(e)
     } finally {
        fin.close()
     }
   }
}
