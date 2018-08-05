package org.rwtodd.bascat

// Unprotect is a decrypter for protected BAS files.
// I found the algorithm in a python program ("PC-BASIC"),
//    (  http://sourceforge.net/p/pcbasic/wiki/Home/  )
// ... but the algorithm was published in:
// The Cryptogram computer supplement #19, American Cryptogram Association, Summer 1994

// Basically there is a 13-byte and an 11-byte key, which are BOTH applied
// in a cycle across the bytes of the input.  Also, a reversed 11-index is subtracted
// from the byte, while a reversed 13-index is added to the byte. By 'reversed', I
// mean that as the 11-index goes from 0 to 10, the reversed index goes from 11 to 1.
object Unprotect {
  private val key13 = Array(0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83, 
                            0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A)
  private val key11 = Array(0x1E, 0x1D, 0xC4, 0x77, 0x26, 
                            0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C)

  def apply(buf: Array[Byte]) = {
    var idx13 = 0
    var idx11 = 0
 
    buf(0) = 0xff.toByte  // mark the buffer as unprotected
    
    for( idx <- 1 until buf.length ) {
      val decoded = (buf(idx) - (11 - idx11) ^ key11(idx11) ^ key13(idx13)) + 
                      (13 - idx13) 
      buf(idx) = decoded.toByte
      idx11 = if (idx11 == 10) 0 else idx11 + 1 
      idx13 = if (idx13 == 12) 0 else idx13 + 1 
    }
    buf
  }
}

// A class that knows how to pull various binary data bits out of an array of Byte.
class BinReader(buf: Array[Byte]) {
  import java.math.{BigDecimal,MathContext}

  private var idx = 0
  private val last2 = buf.length - 1

  def atEOF = idx >= buf.length
  def peek(v: Byte) = !atEOF && buf(idx) == v
  def peek2(v1: Byte, v2: Byte) = (idx < last2) && buf(idx) == v1 && buf(idx+1) == v2
  def skip(amt: Int) = { idx += amt }
  def reset() = { idx = 1 }

  def readByte(): Int = {
    val ans = if (idx < buf.length) (buf(idx) & 0xFF) else 0
    idx += 1
    ans
  }

  def readU16(): Int = {
     val ans = if (idx < last2) ( (buf(idx) & 0xFF) | ((buf(idx+1) & 0xFF) << 8) ) else 0
     idx += 2
     ans
  }

  def readS16(): Int = readU16().toShort.toInt

  // Read a MBF32 floating-point number and build a double out of it.  NB: we don't depend on IEEE float formats, 
  // using BigDecimals instead.
  def readF32(): Double = { 
     val bs0 = readByte() 
     val bs1 = readByte() 
     val bs2 = readByte() 
     val bs3 = readByte() 
     if (bs3 == 0) return 0.0

     val sign = new BigDecimal( if ((0x80 & bs2) == 0)  1 else -1, MathContext.UNLIMITED )
     val exp = bs3 - 129
     val TWO = new BigDecimal(2,MathContext.UNLIMITED)
     val expt = if (exp < 0) BigDecimal.ONE.divide(TWO.pow(-exp,MathContext.UNLIMITED),MathContext.UNLIMITED) else TWO.pow(exp,MathContext.UNLIMITED)
     val scand = new BigDecimal( bs0 | (bs1 << 8) | ((bs2 & 0x7f) << 16), MathContext.UNLIMITED )
     sign.multiply(scand.divide(new BigDecimal(0x800000L,MathContext.UNLIMITED)).add(BigDecimal.ONE)).multiply(expt).doubleValue()
  }

  // Read a MBF64 floating-point number and build a double out of it.  NB: we don't depend on IEEE float formats, 
  // using BigDecimals instead.
  def readF64(): Double = { 
     val bs0 = readByte().toLong
     val bs1 = readByte().toLong 
     val bs2 = readByte().toLong 
     val bs3 = readByte().toLong 
     val bs4 = readByte().toLong 
     val bs5 = readByte().toLong 
     val bs6 = readByte().toLong 
     val bs7 = readByte() 
     if (bs7 == 0) return 0.0

     val sign = new BigDecimal( if ((0x80 & bs6) == 0) 1 else -1, MathContext.UNLIMITED )
     val exp = bs7 - 129
     val TWO = new BigDecimal(2,MathContext.UNLIMITED)
     val expt = if (exp < 0) BigDecimal.ONE.divide(TWO.pow(-exp,MathContext.UNLIMITED),MathContext.UNLIMITED) else TWO.pow(exp,MathContext.UNLIMITED)
     val scand = new BigDecimal( 
         bs0 | (bs1 << 8L) | (bs2 << 16L) |
         (bs3 << 24L) | (bs4 << 32L) | (bs5 << 40L) |
         ((bs6 & 0x7f) << 48L) 
       , MathContext.UNLIMITED )
     sign.multiply(
             scand.divide(new BigDecimal(0x80000000000000L,MathContext.UNLIMITED)).add(BigDecimal.ONE)).multiply(expt,MathContext.UNLIMITED).doubleValue()
  }
}

// This is the class that parses out tokens in the GWBAS file.  First, it detects encrypted files 
// and Unprotect()'s them.  Then, when printAllLines() is called, the tokens are converted to
// strings sent through `out`, one at a time.
class BasCat(buf: Array[Byte], out: java.io.PrintStream) {

   val rdr = new BinReader(buf)
   rdr.readByte() match {
      case 0xff => /* nothing */
      case 0xfe => Unprotect(buf)
      case _    => throw new Exception("Bad 1st Byte!")
   }

   // interpret a single token, writing it to `out`, and return TRUE if the
   // line should have more tokens.
   def printToken(): Boolean = {
     var b = rdr.readByte()
     if (b >= 0xfd) b = (b << 8) | (rdr.readByte())

     var hasMore = true
     b match {
         // The first three cases clean up sequences in the token stream:
         // :ELSE => ELSE 
         case 0x3A if rdr.peek(0xA1.toByte) => { out.print("ELSE"); rdr.skip(1) }
         // :REM' => '  
         case 0x3A if rdr.peek2(0x8F.toByte,0xD9.toByte) => { out.print('\''); rdr.skip(2) }
         // WHILE+ => WHILE 
         case 0xB1 if rdr.peek(0xE9.toByte) => { out.print("WHILE"); rdr.skip(1) }

         // The following cases get special treatment, usually to format numbers.
         case 0x00 => { out.println(); hasMore = false; }   // EOL
         case 0x0B => out.printf("&O%o", int2Integer(rdr.readS16())) // OCTAL
         case 0x0C => out.printf("&H%X", int2Integer(rdr.readS16())) // HEX
         case 0x0E => out.print(rdr.readU16())    // DECIMAL UNSIGNED SHORT
         case 0x0F => out.print(rdr.readByte())   // DECIMAL UNSIGNED BYTE
         case x if (x >= 0x20 && x <= 0x7E) => out.print(x.toChar)  // SINGLE CHAR
         case 0x1C => out.print(rdr.readS16())    // DECIMAL SIGNED SHORT
         case 0x1D => out.printf("%g",double2Double(rdr.readF32()))  // FLOAT 32
         case 0x1F => out.printf("%g",double2Double(rdr.readF64()))  // FLOAT 64
 
         // The rest are just tokens that map directly to strings in BasCat.Tokens.
         case x if (x >= 0x11 && x <= 0x1B) => out.print(BasCat.Tokens(x - 0x11)) 
         case x if (x >= 0x81 && x <= 0xF4) => out.print(BasCat.Tokens(x - 118))  
         case x if (x >= 0xFD81 && x <= 0xFD8B) => out.print(BasCat.Tokens(x - 64770))
         case x if (x >= 0xFE81 && x <= 0xFEA8) => out.print(BasCat.Tokens(x - 65015))
         case x if (x >= 0xFF81 && x <= 0xFFA5) => out.print(BasCat.Tokens(x - 65231))

         // Nothing else to do but complain loudly in the stream:
         case _ => out.printf("<UNK {%d}!>", int2Integer(b))
     }
     hasMore
   }

   // Print all the lines of the GWBAS file through `out`.
   def printAllLines() : Unit = {
       rdr.reset()
       while(!rdr.atEOF) {
          if (rdr.readU16() == 0) return
          out.print(rdr.readU16())
          out.print("  ")
          while(printToken()) { /* nothing */ }
       }
   }
}

object BasCat {

   // Just a convenience method to use the BasCat class in typical fashion 
   def apply(buf: Array[Byte], output: java.io.PrintStream) = {
       new BasCat(buf,output).printAllLines()
   }

   // A main method to let us use BasCat from the command line.
   def main(args: Array[String]) = {
       import java.nio.file.{Files,Paths}
       if (args.length == 2)
          BasCat(Files.readAllBytes(Paths.get(args(1))), System.out) 
       else
          System.err.println("USAGE: bascat <filename>")
   }

   val Tokens = Array(
      /* 0x11 - 0x1B */
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
      /* 0x81 - 0x90 */
      "END", "FOR", "NEXT", "DATA", "INPUT", "DIM", "READ", "LET",
      "GOTO", "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM", "STOP",
      /* 0x91 - 0xA0 */
      "PRINT", "CLEAR", "LIST", "NEW", "ON", "WAIT", "DEF", "POKE",
      "CONT", "<0x9A!>", "<0x9B!>", "OUT", "LPRINT", "LLIST", "<0x9F!>", "WIDTH",
      /* 0xA1 - 0xB0 */
      "ELSE", "TRON", "TROFF", "SWAP", "ERASE", "EDIT", "ERROR", "RESUME",
      "DELETE", "AUTO", "RENUM", "DEFSTR", "DEFINT", "DEFSNG", "DEFDBL", "LINE",
      /* 0xB1 - 0xC0 */
      "WHILE", "WEND", "CALL", "<0xB4!>", "<0xB5!>", "<0xB6!>", "WRITE", "OPTION",
      "RANDOMIZE", "OPEN", "CLOSE", "LOAD", "MERGE", "SAVE", "COLOR", "CLS",
      /* 0xC1 - 0xD0 */
      "MOTOR", "BSAVE", "BLOAD", "SOUND", "BEEP", "PSET", "PRESET", "SCREEN",
      "KEY", "LOCATE", "<0xCB!>", "TO", "THEN", "TAB(", "STEP", "USR",
      /* 0xD1 - 0xE0 */
      "FN", "SPC(", "NOT", "ERL", "ERR", "STRING$", "USING", "INSTR",
      "'", "VARPTR", "CSRLIN", "POINT", "OFF", "INKEY$", "<0xDF!>", "<0xE0!>",
      /* 0xE1 - 0xF0 */
      "<0xE1!>", "<0xE2!>", "<0xE3!>", "<0xE4!>", "<0xE5!>", ">", "=", "<",
      "+", "-", "*", "/", "^", "AND", "OR", "XOR",
      /* 0xF1 - 0xf4 */
      "EQV", "IMP", "MOD", "\\",
      /* 0xFD81 - 0xFD8B */
      "CVI", "CVS", "CVD", "MKI$", "MKS$", "MKD$", "<0xFD87!>", "<0xFD88!>",
      "<0xFD89!>", "<0xFD8A!>", "EXTERR",
      /* 0xFE81 - 0xFE90 */
      "FILES", "FIELD", "SYSTEM", "NAME", "LSET", "RSET", "KILL", "PUT",
      "GET", "RESET", "COMMON", "CHAIN", "DATE$", "TIME$", "PAINT", "COM",
      /* 0xFE91 - 0xFEA0 */
      "CIRCLE", "DRAW", "PLAY", "TIMER", "ERDEV", "IOCTL", "CHDIR", "MKDIR",
      "RMDIR", "SHELL", "ENVIRON", "VIEW", "WINDOW", "PMAP", "PALETTE", "LCOPY",
      /* 0xFEA1 - 0xFEA8 */
      "CALLS", "<0xFEA2!>", "<0xFEA3!>", "NOISE", "PCOPY", "TERM", "LOCK", "UNLOCK",
      /* 0xFF81 - 0xFE90 */
      "LEFT$", "RIGHT$", "MID$", "SGN", "INT", "ABS", "SQR", "RND",
      "SIN", "LOG", "EXP", "COS", "TAN", "ATN", "FRE", "INP",
      /* 0xFF91 - 0xFEA0 */
      "POS", "LEN", "STR$", "VAL", "ASC", "CHR$", "PEEK", "SPACE$",
      "OCT$", "HEX$", "LPOS", "CINT", "CSNG", "CDBL", "FIX", "PEN",
      /* 0xFFA1 - 0xFFA5 */
      "STICK", "STRIG", "EOF", "LOC", "LOF"
   )
}
