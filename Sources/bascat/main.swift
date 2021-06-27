// B A S C A T -- -- Decode tokenized GW-BASIC files
import Foundation

let TOKENS = [
    /* 0x11 - 0x1B */
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",

    // --- *** BREAK *** ---
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

    // --- *** BREAK *** ---
    /* 0xFD81 - 0xFD8B */
    "CVI", "CVS", "CVD", "MKI$", "MKS$", "MKD$", "<0xFD87!>", "<0xFD88!>",
    "<0xFD89!>", "<0xFD8A!>", "EXTERR",

    // --- *** BREAK *** ---
    /* 0xFE81 - 0xFE90 */
    "FILES", "FIELD", "SYSTEM", "NAME", "LSET", "RSET", "KILL", "PUT",
    "GET", "RESET", "COMMON", "CHAIN", "DATE$", "TIME$", "PAINT", "COM",

    /* 0xFE91 - 0xFEA0 */
    "CIRCLE", "DRAW", "PLAY", "TIMER", "ERDEV", "IOCTL", "CHDIR", "MKDIR",
    "RMDIR", "SHELL", "ENVIRON", "VIEW", "WINDOW", "PMAP", "PALETTE", "LCOPY",

    /* 0xFEA1 - 0xFEA8 */
    "CALLS", "<0xFEA2!>", "<0xFEA3!>", "NOISE", "PCOPY", "TERM", "LOCK", "UNLOCK",

    // --- *** BREAK *** ---
    /* 0xFF81 - 0xFE90 */
    "LEFT$", "RIGHT$", "MID$", "SGN", "INT", "ABS", "SQR", "RND",
    "SIN", "LOG", "EXP", "COS", "TAN", "ATN", "FRE", "INP",

    /* 0xFF91 - 0xFEA0 */
    "POS", "LEN", "STR$", "VAL", "ASC", "CHR$", "PEEK", "SPACE$",
    "OCT$", "HEX$", "LPOS", "CINT", "CSNG", "CDBL", "FIX", "PEN",

    /* 0xFFA1 - 0xFFA5 */
    "STICK", "STRIG", "EOF", "LOC", "LOF"
]

func decrypt_buffer(_ buf: inout [UInt8]) {
  let KEY11: [UInt8]  = [0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C]
  let KEY13: [UInt8]  = [0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A]
  buf[0] = 0xff  // decrypted!
  for i in 1..<buf.count {
    let idx11 : UInt8 = UInt8((i - 1) % 11)
    let idx13 : UInt8 = UInt8((i - 1) % 13)
    buf[i] = ((buf[i] &- (11 &- idx11))  ^ 
              (KEY11[Int(idx11)]) ^ 
              (KEY13[Int(idx13)])) &+ (13 &- idx13)
  }
}

struct DataReader {
   let src      : [UInt8]
   let count    : Int
   var idx      : Int = 1

   init(fromBytes src: [UInt8]) {
      self.src = src
      count    = src.count
   }

   init?(fromFile fname: String) {
     if let data = NSData(contentsOfFile: fname) {
        if data.length == 0 { return nil }

        var buffer = [UInt8](repeating: 0, count: data.length)
        data.getBytes(&buffer, length: data.length)
        if buffer[0] == 0xfe {
           decrypt_buffer(&buffer)
        }
        if buffer[0] == 0xff {
	   src   = buffer
           count = src.count
        } else {
	   return nil
	}

     } else {
        return nil  // file didn't open or didn't look like GWBASIC
     }

   }

   mutating func read_u8() -> UInt8 {
      if idx < count {
         let answer = src[idx]
	 idx += 1
	 return answer
      }
      return 0
   }

   // Peek ahead a byte.
   func peek(next val: UInt8) -> Bool {
      return (idx < count) && (src[idx] == val)
   }

   // Peek ahead two bytes.
   func peek(next val1: UInt8, and val2: UInt8) -> Bool {
      return (idx+1 < count) && 
             (src[idx] ==  val1) &&
	     (src[idx+1] == val2)
   }

   // Read a little-endian i16 from a byte iterator.
   mutating func read_i16() -> Int16 {
     let b1 = Int16(read_u8())
     let b2 = Int16(read_u8()) 
     return (b2 << 8) | b1 
   }

   mutating func read_u16() -> UInt16 {
     let b1 = UInt16(read_u8())
     let b2 = UInt16(read_u8()) 
     return (b2 << 8) | b1 
   }

   mutating func read_f32() -> Float {
     let a = UInt64(read_u8())
     let b = UInt64(read_u8())
     let c = UInt64(read_u8())
     let d = Int(read_u8())
     if d == 0 { return 0.0 }
     let sign : FloatingPointSign = (c & 0x80) == 0 ? .plus : .minus
     let exp = d - 129
     let scand = Double( ((c|0x80) << 16) | (b << 8) | a ) /
                 Double(0x80_0000)
     return Float(Double(sign: sign, exponent: exp, significand: scand))
   }

   mutating func read_f64() -> Double {
     let a = UInt64(read_u8())
     let b = UInt64(read_u8())
     let c = UInt64(read_u8())
     let d = UInt64(read_u8())
     let e = UInt64(read_u8())
     let f = UInt64(read_u8())
     let g = UInt64(read_u8())
     let h = Int(read_u8())
     if h == 0 { return 0.0 }
     let sign : FloatingPointSign = (g & 0x80) == 0 ? .plus : .minus
     let exp = h - 129
     let scand_int = ((g|0x80) << 48) | (f << 40) | (e << 32) |
                     (d << 24) | (c << 16) | (b << 8) | a
     let scand = Double( scand_int ) / Double( 0x80_0000_0000_0000 )
     return Double(sign: sign, exponent: exp, significand: scand)
   }

   mutating func skip(_ n: Int) { idx += n }
}

func parse_opcode(from dr: inout DataReader,
                  to buf: inout String) -> Bool {
  let num = UInt16(dr.read_u8())
  let opcode = num >= 0xfd ? (num << 8)|UInt16(dr.read_u8()) : num

  if opcode == 0x3A && dr.peek(next: 0xA1) {
     buf.append("ELSE")
     dr.skip(1)
  } else if opcode == 0x3A && dr.peek(next: 0x8F, and: 0xD9) {
     buf.append("'")
     dr.skip(2)
  } else if opcode == 0xB1 && dr.peek(next: 0xE9) {
     buf.append("WHILE")
     dr.skip(1)
  } else {
     switch opcode {
     case 0x00: break // do nothing
     case 0x0B: buf.append(String(format: "&O%o", dr.read_i16()))
     case 0x0C: buf.append(String(format: "&H%X", dr.read_i16()))
     case 0x0E: print(dr.read_u16(), terminator: "", to: &buf)
     case 0x0F: print(dr.read_u8(), terminator: "", to: &buf)
     case 0x11...0x1B: buf.append(TOKENS[Int(opcode - 0x11)])
     case 0x1C: print(dr.read_i16(), terminator: "", to: &buf)
     case 0x1D: print(dr.read_f32(), terminator: "", to: &buf)
     case 0x1F: print(dr.read_f64(), terminator: "", to: &buf)
     case 0x20...0x7E: buf.append(Character(UnicodeScalar(opcode)!)) 
     case 0x81...0xF4: buf.append(TOKENS[Int(num - 118)])
     case 0xFD81...0xFD8B: buf.append(TOKENS[Int(opcode - 64770)])
     case 0xFE81...0xFEA8: buf.append(TOKENS[Int(opcode - 65015)])
     case 0xFF81...0xFFA5: buf.append(TOKENS[Int(opcode - 65231)])
     default: buf.append(String(format: "<UNK! %04X>", opcode))
     }
  }
  return (opcode != 0)
}

func read_line(from dr: inout DataReader, to buf: inout String) {
  if dr.read_u16() != 0 {
     print(dr.read_u16(), terminator: "  ", to: &buf)
     while parse_opcode(from: &dr, to: &buf) {
        // nothing!
     }
  }
}

// ----------------------------------------------------------------------
// M A I N  P R O G R A M
// ----------------------------------------------------------------------
if CommandLine.argc < 2 {
   print("Need a file name!")
   exit(1)
}

if var dr = DataReader(fromFile: CommandLine.arguments[1]) {

  var buffer = ""
  while true {
     read_line(from: &dr, to: &buffer)
     if buffer.isEmpty { break }
     print(buffer)
     buffer.removeAll(keepingCapacity: true)
  }

} else {

   print("Could not load file! ", CommandLine.arguments[1])
   exit(1)

}

