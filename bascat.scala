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
//
// NOTE: this is a destructive operation on the original array
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

case class StreamState[A](run: Stream[Byte] => (A, Stream[Byte])) {
  def map[B] (f: A => B): StreamState[B] = StreamState (s1 => {
      val (a, s2) = run (s1)
      (f (a), s2)
  })

  def flatMap[B] (f: A => StreamState[B]): StreamState[B] = StreamState (s1 => {
      val (a, s2) = run (s1)
      f (a) run s2
  })
}

object StreamState {
  import java.math.{BigDecimal,MathContext}

  // These methods read (or peek at) the data at the head of the state
  val peekByte = StreamState( s => (s.head & 0xff, s) )
  val peekU16BigEndian = StreamState( s => (((s(0) & 0xff) << 8)|(s(1) & 0xff), s) )
  val readByte = StreamState( s => (s.head & 0xff, s.tail) )
  def readBytes(n: Int) = StreamState( s => (s.take(n).toArray, s.drop(n)) )
  val readU16 = for { bs <- readBytes(2) } yield ((bs(1) & 0xff) << 8)|(bs(0) & 0xff)
  val readS16 = readU16 map { _.toShort.toInt }
  val readU16BigEndian = for { bs <- readBytes(2) } yield ((bs(0) & 0xff) << 8)|(bs(1) & 0xff)
  def skip(n: Int) = StreamState(s => (Unit, s drop n))

  // Read a MBF32 floating-point number and build a double out of it.  NB: we don't depend on IEEE float formats, 
  // using BigDecimals instead.
  val readF32 = for { 
        bs <- readBytes(4)
     } yield {
        if (bs(3) == 0) 0.0 else {
          val sign = new BigDecimal( if ((0x80 & bs(2)) == 0)  1 else -1, MathContext.UNLIMITED )
          val exp = (bs(3) & 0xff) - 129
          val TWO = new BigDecimal(2,MathContext.UNLIMITED)
          val expt = if (exp < 0) BigDecimal.ONE.divide(TWO.pow(-exp,MathContext.UNLIMITED),MathContext.UNLIMITED) else TWO.pow(exp,MathContext.UNLIMITED)
          val scand = new BigDecimal( (bs(0) & 0xff) | ((bs(1) & 0xff) << 8) | ((bs(2) & 0x7f) << 16), MathContext.UNLIMITED )
          sign.multiply(scand.divide(new BigDecimal(0x800000L,MathContext.UNLIMITED)).add(BigDecimal.ONE)).multiply(expt).doubleValue()
        }
     }

  // Read a MBF64 floating-point number and build a double out of it.  NB: we don't depend on IEEE float formats, 
  // using BigDecimals instead.
  val readF64 = for { 
        bs <- readBytes(8) map { bs => bs.map( x => (x & 0xff).toLong ) }
     } yield { 
       if (bs(7) == 0) 0.0 else {
         val sign = new BigDecimal( if ((0x80 & bs(6)) == 0) 1 else -1, MathContext.UNLIMITED )
         val exp = bs(7).toInt - 129
         val TWO = new BigDecimal(2,MathContext.UNLIMITED)
         val expt = if (exp < 0) BigDecimal.ONE.divide(TWO.pow(-exp,MathContext.UNLIMITED),MathContext.UNLIMITED) else TWO.pow(exp,MathContext.UNLIMITED)
         val scand = new BigDecimal( 
             bs(0) | (bs(1) << 8L) | (bs(2) << 16L) |
             (bs(3) << 24L) | (bs(4) << 32L) | (bs(5) << 40L) |
             ((bs(6) & 0x7f) << 48L) 
           , MathContext.UNLIMITED )
         sign.multiply(
            scand.divide(new BigDecimal(0x80000000000000L,MathContext.UNLIMITED)).add(BigDecimal.ONE)).multiply(expt,MathContext.UNLIMITED).doubleValue()
       }
     }

  // result is the haskell monadic 'return/pure'
  def result[T](t: T) = StreamState { s => (t,s) }

  // run a state-machine which returns options, collecting the results until it
  // returns None
  def collect[T](engine: StreamState[Option[T]]): StreamState[Vector[T]] = StreamState(s=> {
     var state = s
     val builder = Vector.newBuilder[T]
     var more = true
     while(more) {
        (engine run state) match {
          case (Some(t), s2) => { builder += t; state = s2 }
          case (None, s2) => { more = false; state = s2 }
        }
     }
     (builder.result, state)
  })
}

object BasCat {
   import StreamState._

   def parseToken(token: Int): StreamState[Option[String]] = 
     if (token == 0) result(None) else 
     for {
       lookahead <- peekU16BigEndian  // get the next two bytes into an Int
       parsed    <- token match {
         // The first three cases clean up sequences in the token stream:
         // :ELSE => ELSE 
         case 0x3A if ((lookahead & 0xff00) == 0xA100) => skip(1) map { _ => "ELSE" }
         // :REM' => '  
         case 0x3A if (lookahead == 0x8FD9) => skip(2) map { _ => "'" }
         // WHILE+ => WHILE 
         case 0xB1 if ((lookahead & 0xff00) == 0xE900) => skip(1) map { _ => "WHILE" }

         // The following cases get special treatment, usually to format numbers.
         case 0x0B => readS16 map { x => f"&O$x%o" } // OCTAL
         case 0x0C => readS16 map { x => f"&H$x%X" } // HEX
         case 0x0E => readU16 map { x => x.toString} // DECIMAL UNSIGNED SHORT
         case 0x0F => readByte map { x => x.toString} // DECIMAL UNSIGNED BYTE
         case x if (x >= 0x20 && x <= 0x7E) => result(x.toChar.toString)  // SINGLE CHAR
         case 0x1C => readS16 map { x => x.toString}   // DECIMAL SIGNED SHORT
         case 0x1D => readF32 map { x => f"$x%g" } // FLOAT 32
         case 0x1F => readF64 map { x => f"$x%g" } // FLOAT 64

         // The rest are just tokens that map directly to strings in BasCat.Tokens.
         case x if (x >= 0x11 && x <= 0x1B) => result(Tokens(x - 0x11)) 
         case x if (x >= 0x81 && x <= 0xF4) => result(Tokens(x - 118))  
         case x if (x >= 0xFD81 && x <= 0xFD8B) => result(Tokens(x - 64770))
         case x if (x >= 0xFE81 && x <= 0xFEA8) => result(Tokens(x - 65015))
         case x if (x >= 0xFF81 && x <= 0xFFA5) => result(Tokens(x - 65231))

         // Nothing else to do but complain loudly in the stream:
         case _ => result(f"<UNK $token%X!>")
       } 
     } yield Some(parsed)

   val lineToken: StreamState[Option[String]] = for {
      b      <- peekByte
      token  <- if (b < 0xfd) readByte else readU16BigEndian
      parsed <- parseToken(token)
   } yield parsed

   val generateLine: StreamState[Option[String]] = for {
     ptr <- readU16
     line  <- if (ptr == 0) result(None) else
              for {
                lineno <- readU16
                sb = new StringBuilder().append(lineno).append("  ")
                toks   <- collect(lineToken)
              } yield Some(toks.addString(sb).toString)
   }  yield line

   def generateLines(bs: Stream[Byte]): Stream[String] = {
      val (ln, bs2) = generateLine.run(bs)
      ln match {
         case Some(str) => str #:: generateLines(bs2)
         case None      => Stream.empty
      }
   }

   // Just a convenience method to use the BasCat class in typical fashion 
   def apply(buf: Array[Byte], output: java.io.PrintStream) = {
     generateLines(
       Stream.concat(
         (buf(0) & 0xff) match {
           case 0xff => buf.toStream.tail
           case 0xfe => Unprotect(buf).toStream.tail
           case _    => throw new Exception("Bad 1st Byte!")
         },
         Stream.continually(0.toByte))
     ).foreach { output.println(_) }
   }

   // A main method to let us use BasCat from the command line.
   def main(args: Array[String]) = {
       import java.nio.file.{Files,Paths}
      if (args.length == 1)
          BasCat(Files.readAllBytes(Paths.get(args(0))), System.out) 
       else
          System.err.println("USAGE: bascat <filename>")
   }

   val Tokens = IndexedSeq(
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
