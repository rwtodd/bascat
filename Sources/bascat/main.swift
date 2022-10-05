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

func decrypt_buffer(_ buf: UnsafeMutableRawBufferPointer) {
  let KEY11: [UInt8]  = [0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C]
  let KEY13: [UInt8]  = [0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A]
  buf.storeBytes(of: 0xff, toByteOffset: 0, as: UInt8.self)  // decrypted!
  for i in 1..<buf.count {
    let idx11 : UInt8 = UInt8((i - 1) % 11)
    let idx13 : UInt8 = UInt8((i - 1) % 13)
    let decrypted : UInt8 = ((buf[i] &- (11 &- idx11))  ^ 
                             (KEY11[Int(idx11)]) ^ 
                             (KEY13[Int(idx13)])) &+ (13 &- idx13)
    buf.storeBytes(of: decrypted, toByteOffset: i, as: UInt8.self)
  }
}

class DataReader {
   private let src : UnsafeRawBufferPointer 
   private var idx : Int = 1

   init(_ buffer: UnsafeRawBufferPointer) {
       src = buffer
   }

   func read_u8() -> UInt8 {
     let answer = (idx < src.count) ? src[idx] : 0 
     idx += 1
     return answer
   }

   // Peek ahead a byte.
   func peek(next val: UInt8) -> Bool { idx < src.count && src[idx] == val }

   // Peek ahead two bytes.
   func peek(next val1: UInt8, and val2: UInt8) -> Bool {
      idx < src.count - 1 && src[idx] ==  val1 && src[idx+1] == val2
   }

   // Read a little-endian i16 from a byte iterator.
   func read_i16() -> Int16 {
     guard idx < src.count - 1 else { return 0 }
     let b1 = Int16(src[idx])
     let b2 = Int16(src[idx+1]) 
     idx += 2
     return (b2 << 8) | b1 
   }

   func read_u16() -> UInt16 {
     guard idx < src.count - 1 else { return 0 }
     let b1 = UInt16(src[idx])
     let b2 = UInt16(src[idx+1]) 
     idx += 2
     return (b2 << 8) | b1 
   }

   func read_f32() -> Float {
     guard idx < src.count - 3 else { return 0.0 }
     let a = UInt64(src[idx]),
         b = UInt64(src[idx+1]),
         c = UInt64(src[idx+2]),
         d = Int(src[idx+3])
     idx += 4
     if d == 0 { return 0.0 }
     let sign : FloatingPointSign = (c & 0x80) == 0 ? .plus : .minus
     let exp = d - 129
     let scand = Double( ((c|0x80) << 16) | (b << 8) | a ) /
                 Double(0x80_0000)
     return Float(Double(sign: sign, exponent: exp, significand: scand))
   }

   func read_f64() -> Double {
     guard idx < src.count - 7 else { return 0.0 }
     let a = UInt64(src[idx]),
         b = UInt64(src[idx+1]),
         c = UInt64(src[idx+2]),
         d = UInt64(src[idx+3]),
         e = UInt64(src[idx+4]),
         f = UInt64(src[idx+6]),
         g = UInt64(src[idx+6]),
         h = Int(src[idx+7])
     idx += 8
     if h == 0 { return 0.0 }
     let sign : FloatingPointSign = (g & 0x80) == 0 ? .plus : .minus
     let exp = h - 129
     let scand_int = ((g|0x80) << 48) | (f << 40) | (e << 32) |
                     (d << 24) | (c << 16) | (b << 8) | a
     let scand = Double( scand_int ) / Double( 0x80_0000_0000_0000 )
     return Double(sign: sign, exponent: exp, significand: scand)
   }

   func skip(_ n: Int) { idx += n }
}

func parse_opcode(from dr: DataReader,
                  to buf: inout String) -> Bool {
  let num = UInt16(dr.read_u8())
  let opcode = num >= 0xfd ? (num << 8)|UInt16(dr.read_u8()) : num

  switch opcode {
  case 0x3A where dr.peek(next: 0xA1): 
     buf.append("ELSE")
     dr.skip(1)
  case 0x3A where dr.peek(next: 0x8F, and: 0xD9):
     buf.append("'")
     dr.skip(2)
  case 0xB1 where dr.peek(next: 0xE9): 
     buf.append("WHILE")
     dr.skip(1)
  case 0x00           : break // do nothing
  case 0x0A           : buf.append(String(format: "&O%o", dr.read_i16()))
  case 0x0C           : buf.append(String(format: "&H%X", dr.read_i16()))
  case 0x0E           : print(dr.read_u16(), terminator: "", to: &buf)
  case 0x0F           : print(dr.read_u8(), terminator: "", to: &buf)
  case 0x11...0x1B    : buf.append(TOKENS[Int(opcode - 0x11)])
  case 0x1C           : print(dr.read_i16(), terminator: "", to: &buf)
  case 0x1D           : print(dr.read_f32(), terminator: "", to: &buf)
  case 0x1F           : print(dr.read_f64(), terminator: "", to: &buf)
  case 0x20...0x7E    : buf.append(Character(UnicodeScalar(opcode)!))
  case 0x81...0xF4    : buf.append(TOKENS[Int(num - 118)])
  case 0xFD81...0xFD8B: buf.append(TOKENS[Int(opcode - 64770)])
  case 0xFE81...0xFEA8: buf.append(TOKENS[Int(opcode - 65015)])
  case 0xFF81...0xFFA5: buf.append(TOKENS[Int(opcode - 65231)])
  default             : buf.append(String(format: "<UNK! %04X>", opcode))
  }
  return (opcode != 0)
}

func read_line(from dr: DataReader, to buf: inout String) {
  if dr.read_u16() != 0 {
     print(dr.read_u16(), terminator: "  ", to: &buf)
     while parse_opcode(from: dr, to: &buf) {  }
  }
}

// ----------------------------------------------------------------------
// M A I N  P R O G R A M
// ----------------------------------------------------------------------
guard CommandLine.argc >= 2 else {
   print("Need a file name!")
   exit(1)
}

// get the bytes out of the file
guard var data = try? NSMutableData(contentsOfFile: CommandLine.arguments[1]) as Data, data.count > 0 else {
   print("Could not load file! ", CommandLine.arguments[1])
   exit(1)
}

// decrypt the file if needed
if data[0] == 0xfe {
   data.withUnsafeMutableBytes { decrypt_buffer($0) }
}

// make sure it looks like a GWBAS file based on the first byte
// (not super reliable, especially since a common UTF BOM starts with 0xff)
guard data[0] == 0xff else {
   print("Bad file data (not GWBAS file??)! ")
   exit(1)
}

data.withUnsafeBytes { bytes in
  var buffer = "" ; buffer.reserveCapacity(128)
  let dr = DataReader(bytes) 
  while true {
    read_line(from: dr, to: &buffer)
    if buffer.isEmpty { break }
    print(buffer)
    buffer.removeAll(keepingCapacity: true)
  }
}
