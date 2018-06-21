use std::env::args;
use std::fs::File;
use std::io::{self, BufReader,Error,ErrorKind, StdoutLock};
use std::io::prelude::*;

const TOKENS : [&str;215] = [
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
];


type Bytes = Iterator<Item=std::io::Result<u8>>;

// -- Decrypting Iterator
const KEY11 : [u8; 11] = [0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C];
const KEY13 : [u8; 13] = [0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A];

struct DecryptedBytes {
   unenc: Box<Bytes>,
   idx11: usize,
   idx13: usize,
}

impl Iterator for DecryptedBytes {
   type Item = std::io::Result<u8>;

   fn next(&mut self) -> Option<std::io::Result<u8>> {
      if let Some(Ok(x)) = self.unenc.next() {
         let result = ((x - (11 - self.idx11 as u8))  ^ 
                       (KEY11[self.idx11]) ^ 
                       (KEY13[self.idx13])) + (13 - self.idx13 as u8);
         self.idx11 = (self.idx11 + 1) % 11;
         self.idx13 = (self.idx13 + 1) % 13;
         Some(Ok(result))
      } else {
         Some(Ok(0u8)) 
      }
   }
}

#[inline]
fn read_u8(b: &mut Bytes) -> u8 {
  if let Some(Ok(x)) = b.next() { x } else { 0u8 }
}

fn read_i16(b: &mut Bytes) -> i16 {
  let b1 = read_u8(b) as i16;
  let b2 = read_u8(b) as i16;
  (b2 << 8) | b1 
}

fn read_u16(b: &mut Bytes) -> u16 {
  let b1 = read_u8(b) as u16;
  let b2 = read_u8(b) as u16;
  (b2 << 8) | b1 
}
fn read_f32(b: &mut Bytes) -> f32 {
  let b0 = read_u8(b);
  let b1 = read_u8(b);
  let mut b2 = read_u8(b);
  let mut b3 = read_u8(b);
  if b3 == 0 {
     0.0f32
  } else {
     let sgn = b2 & 0x80;
     let exp = (b3 - 2) & 0xff;
     b3 = sgn | (exp >> 1);
     b2 = ((exp << 7) | (b2 & 0x7f)) & 0xff;
     f32::from_bits((b0 as u32) | (b1 as u32)<<8 | (b2 as u32)<<16 | (b3 as u32)<<24)
  }
}

fn read_f64(b: &mut Bytes) -> f64 {
  let mut bs = [0u8; 8];
  bs[0] = read_u8(b);
  bs[1] = read_u8(b);
  bs[2] = read_u8(b);
  bs[3] = read_u8(b);
  bs[4] = read_u8(b);
  bs[5] = read_u8(b);
  bs[6] = read_u8(b);
  bs[7] = read_u8(b);
  if bs[7] == 0 { 
    0.0f64
  } else {
    let sgn = bs[6] & 0x80;
    let exp = ((bs[3] as u16) - 128 - 1 + 1023) & 0xffff;
    bs[7] = sgn | (((exp >> 4) & 0xff) as u8);
    let mut left_over = ((exp << 4) & 0xff) as u8;
    for idx in (1..7).rev() {
        let tmp = ((bs[idx] << 1) & 0xff) | (bs[idx-1] >> 7);
        bs[idx] = left_over | (tmp >> 4);
        left_over = (tmp << 4) & 0xff;
    }
    let tmp = (bs[0] << 1) & 0xff;
    bs[0] = left_over | (tmp >> 4);
    f64::from_bits((bs[0] as u64) | (bs[1] as u64)<<8 | (bs[2] as u64)<<16 | (bs[3] as u64)<<24 |
                   (bs[4] as u64)<<32 | (bs[5] as u64)<<40 | (bs[6] as u64)<<48 | (bs[7] as u64)<<56)
  }
}

fn get_reader(fname: String) -> std::io::Result<Box<Bytes>> {
    let file = File::open(fname)?;
    let buf_reader = BufReader::new(file);
    let mut bytes = Box::new(buf_reader.bytes()); 
    match bytes.next() {
       Some(Ok(0xff)) => Ok(bytes),
       Some(Ok(0xfe)) => Ok(Box::new(DecryptedBytes {unenc: bytes, idx11: 0, idx13: 0 })),
       Some(Err(e))   => Err(e),
       _              => Err(Error::new(ErrorKind::Other, "not a BAS file!")),
    }
}

struct Token  {
  num: u16,
  desc: String,
}

impl Token {
  fn arbitrary(desc: String) -> Token {
     Token { num: 1, desc: desc }
  }

  fn new(num: u16, b: &mut Bytes) -> Token {
    match num {
      0xfd ... 0xff => Token::new( (num << 8)|(read_u8(b) as u16), b ), 
      0x00 => Token { num: num, desc: String::from("EOF") },
      0x0B => Token { num: num, desc: format!("&O{:o}",read_i16(b)) },
      0x0C => Token { num: num, desc: format!("&H{:x}",read_i16(b)) },
      0x0E => Token { num: num, desc: format!("{}",read_u16(b)) },
      0x0F => Token { num: num, desc: format!("{}",read_u8(b)) },
      0x11 ... 0x1B => Token { num: num, desc: String::from(TOKENS[(num - 0x11) as usize]) },
      0x1C => Token { num: num, desc: format!("{}",read_i16(b)) },
      0x1D => Token { num: num, desc: format!("{}",read_f32(b)) },
      0x1F => Token { num: num, desc: format!("{}",read_f64(b)) },
      0x20 ... 0x7E => Token { num: num, desc: String::from_utf16(&[num]).unwrap() },
      0x81 ... 0xF4 => Token { num: num, desc: String::from(TOKENS[(num - 118) as usize]) },
      0xFD81 ... 0xFD8B => Token { num: num, desc: String::from(TOKENS[(num - 64770) as usize]) },
      0xFE81 ... 0xFEA8 => Token { num: num, desc: String::from(TOKENS[(num - 65015) as usize]) },
      0xFF81 ... 0xFFA5 => Token { num: num, desc: String::from(TOKENS[(num - 65231) as usize]) },
      _ => Token { num: num, desc: format!("<UNK! {:X}>", num) },
   }
 }
}


fn read_line(b: &mut Bytes, buf: &mut Vec<Token>) {
  if read_u16(b) != 0 {
     buf.push( Token::arbitrary(format!("{}  ",read_u16(b))) );
     loop {
        let opcode = read_u8(b) as u16;
        if opcode == 0 { break }
        buf.push( Token::new(opcode, b) )
     }
  }
}

fn display_line(dest: &mut StdoutLock, toks: &mut Vec<Token>) -> std::io::Result<()> {
   let mut idx :usize = 0;
   let max = toks.len();
   while idx < max {
      if ((max-idx)>1) && (toks[idx].num == 0x3A) && (toks[idx+1].num == 0xA1) {
          idx += 1 
      }
      else if ((max-idx)>2) && (toks[idx].num == 0x3A) && (toks[idx+1].num == 0x8F) && (toks[idx+2].num == 0xD9) {
          idx += 2
      } else if ((max-idx)>1) && (toks[idx].num == 0xB1) && (toks[idx+1].num == 0xE9) {
          toks[idx+1].desc = toks[idx].desc.clone();
          idx += 1
      } 
      write!(dest, "{}",toks[idx].desc)?;
      idx += 1;
   }
   return writeln!(dest);
}

fn main() -> std::io::Result<()> {
    let fname = args().nth(1).unwrap_or_else(|| String::from("tour.gwbas"));
    let mut rdr = get_reader(fname)?;
    let stdout = io::stdout();
    let mut handle = stdout.lock();
    let mut line_buf = vec![];
    loop {
       read_line(&mut rdr, &mut line_buf);
       if line_buf.is_empty() { break }
       display_line(&mut handle, &mut line_buf)?;
       line_buf.clear(); 
    }
    Ok(()) 
}
