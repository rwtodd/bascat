use std::env::args;
use std::fs::File;
use std::io::{BufReader,Error,ErrorKind};
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


type Bytes = Box<Iterator<Item=std::io::Result<u8>>>;

fn read_u8(b: &mut Bytes) -> u8 {
  b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8)
}

fn read_i16(b: &mut Bytes) -> i16 {
  let b1 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as i16;
  let b2 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as i16;
  (b2 << 8) | b1 
}

fn read_u16(b: &mut Bytes) -> u16 {
  let b1 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u16;
  let b2 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u16;
  (b2 << 8) | b1 
}
fn read_f32(b: &mut Bytes) -> f32 {
  let b1 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b2 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b3 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b4 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  0.32
}

fn read_f64(b: &mut Bytes) -> f32 {
  let b1 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b2 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b3 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b4 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b5 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b6 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b7 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  let b8 = b.next().unwrap_or(Ok(0u8)).unwrap_or(0u8) as u8;
  0.64
}

fn get_reader(fname: String) -> std::io::Result<Bytes> {
    let file = File::open(fname)?;
    let buf_reader = BufReader::new(file);
    let mut bytes = Box::new(buf_reader.bytes()); 
    match bytes.next() {
       Some(Ok(0xff)) => Ok(bytes),
       Some(Ok(0xfe)) => Err(Error::new(ErrorKind::Other, "encrypted file!")),
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
      0xfd ... 0xff => Token::new( (num << 8)|(read_u8(b) as u16), b ), // TODO: put this at top of match...
      0xFD81 ... 0xFD8B => Token { num: num, desc: String::from(TOKENS[(num - 64770) as usize]) },
      0xFE81 ... 0xFEA8 => Token { num: num, desc: String::from(TOKENS[(num - 65015) as usize]) },
      0xFF81 ... 0xFFA5 => Token { num: num, desc: String::from(TOKENS[(num - 65231) as usize]) },
      _ => Token { num: num, desc: format!("<UNK! {:X}>", num) },
   }
 }
}


fn read_line(b: &mut Bytes) -> Vec<Token> {
  let mut result = Vec::new();
  if read_u16(b) != 0 {
     result.push( Token::arbitrary(format!("{}  ",read_u16(b))) );
     loop {
        let opcode = read_u8(b) as u16;
        if opcode == 0 { break }
        result.push( Token::new(opcode, b) )
     }
  }
  result
}

fn display_line(mut toks: Vec<Token>) {
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
      print!("{}",toks[idx].desc);
      idx += 1
   }
   println!();
}

fn main() -> std::io::Result<()> {
    let fname = args().nth(1).unwrap_or_else(|| String::from("tour.gwbas"));
    let mut rdr = get_reader(fname)?;
    loop {
       let l = read_line(&mut rdr);
       if l.is_empty() { break }
       display_line(l);
    }
    Ok(()) 
}
