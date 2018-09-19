use std::env::args;
use std::fs::File;
use std::fmt::Write;
use std::io::{self, Error, ErrorKind};
use std::io::prelude::*;

/// TOKENS contains string descriptions of the canned opcodes found
/// in GWBAS files.
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


// -- Some support data for the decrypting Iterator
const KEY11 : [u8; 11] = [0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C];
const KEY13 : [u8; 13] = [0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A];
// -- End of support data for the decrypting Iterator

fn decrypt_buffer(buf: &mut Vec<u8>) {
  buf[0] = 0xff;
  for i in 1..buf.len() {
    let idx11 = (i - 1) % 11;
    let idx13 = (i - 1) % 13;
    buf[i] = ((buf[i] - (11 - idx11 as u8))  ^ 
              (KEY11[idx11]) ^ 
              (KEY13[idx13])) + (13 - idx13 as u8);
  }
}

/// Bytes is just a way to track where we are in the buffer.
/// Functions liek read_u8 use it, and return 0 past the end.
/// For translating GW-BASIC, this is safe.
struct Bytes {
   buffer: Vec<u8>,
   index: usize,
}

/// Read a u8 from a byte iterator, returning 0u8 on all
/// errors. Zeros are guaranteed to halt the processing,
/// so there is no harm in this behavior in error cases
/// other than we don't report read errors to the user.
#[inline]
fn read_u8(b: &mut Bytes) -> u8 {
  if b.index < b.buffer.len() {
     let ans = b.buffer[b.index];
     b.index += 1;
     ans 
  } else { 0u8 }
}

/// Peek ahead a byte.
fn peek_one(b: &Bytes, val: u8) -> bool {
   (b.index < b.buffer.len()) && (b.buffer[b.index] == val)
}

/// Peek ahead two bytes.
fn peek_two(b: &Bytes, val1: u8, val2: u8) -> bool {
   (b.index+1 < b.buffer.len()) && 
   (b.buffer[b.index] == val1) && 
   (b.buffer[b.index+1] == val2)
}

/// Read a little-endian i16 from a byte iterator.
fn read_i16(b: &mut Bytes) -> i16 {
  let b1 = read_u8(b) as i16;
  let b2 = read_u8(b) as i16;
  (b2 << 8) | b1 
}

/// Read a little-endian u16 from a byte iterator.
fn read_u16(b: &mut Bytes) -> u16 {
  let b1 = read_u8(b) as u16;
  let b2 = read_u8(b) as u16;
  (b2 << 8) | b1 
}

/// Read a MS MBF-style 32-bit float, and convert it to a modern IEEE float.
/// NB: This function assumes little endian outputs are appropriate.
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

/// Read a MS MBF-style 64-bit float, and convert it to a modern IEEE double.
/// NB: This function assumes little endian outputs are appropriate.
fn read_f64(b: &mut Bytes) -> f64 {
  let mut bs = [0u8; 8];
  for i in 0..8 { bs[i] = read_u8(b); }

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

/// Read the first byte of FNAME, and decrypt the file
/// if necessary.  Then, return the unencrypted bytes. 
fn load_buffer(fname: String) -> std::io::Result<Vec<u8>> {
    let mut buffer = Vec::new();
    let mut file = File::open(fname)?;
    let _ = file.read_to_end(&mut buffer)?;
    match buffer[0] {
       0xff => Ok(buffer),
       0xfe => {
                 decrypt_buffer(&mut buffer);
                 Ok(buffer)
               },
       _    => Err(Error::new(ErrorKind::Other, "not a GWBAS file!")),
    }
}

/// Generate a string from an opcode, which sometimes requires reading
/// deeper into the file.
fn parse_opcode(b: &mut Bytes, buf: &mut String) -> bool {
    let num = read_u8(b) as u16;
    let opcode = if num >= 0xfd {  (num << 8)|(read_u8(b) as u16) } else { num };

    if opcode == 0x3A && peek_one(b,0xA1) {
        buf.push_str("ELSE");
        b.index += 1;
    } else if opcode == 0x3A && peek_two(b,0x8F,0xD9) {
        buf.push_str("'");
        b.index += 2;
    } else if opcode == 0xB1 && peek_one(b,0xE9) {
        buf.push_str("WHILE");
        b.index += 1;
    } else {
      match opcode {
        0x00 => writeln!(buf).unwrap(),
        0x0B => write!(buf,"&O{:o}",read_i16(b)).unwrap(),
        0x0C => write!(buf,"&i{:x}",read_i16(b)).unwrap(),
        0x0E => write!(buf,"{}",read_u16(b)).unwrap(),
        0x0F => write!(buf,"{}",read_u8(b)).unwrap(),
        0x11 ..= 0x1B => buf.push_str(TOKENS[(opcode - 0x11) as usize]),
        0x1C => write!(buf,"{}",read_i16(b)).unwrap(),
        0x1D => write!(buf,"{}",read_f32(b)).unwrap(),
        0x1F => write!(buf,"{}",read_f64(b)).unwrap(),
        0x20 ..= 0x7E => buf.push(char::from(opcode as u8)),
        0x81 ..= 0xF4 => buf.push_str(TOKENS[(num - 118) as usize]),
        0xFD81 ..= 0xFD8B => buf.push_str(TOKENS[(opcode - 64770) as usize]),
        0xFE81 ..= 0xFEA8 => buf.push_str(TOKENS[(opcode - 65015) as usize]),
        0xFF81 ..= 0xFFA5 => buf.push_str(TOKENS[(opcode - 65231) as usize]),
        _ => write!(buf,"<UNK! {:X}>", num).unwrap(),
      }
   }
   opcode != 0
}

/// Fill BUF with tokens representing the next line of the GWBAS file.
/// At the EOF, nothing will be added to BUF.
fn read_line(basic: &mut Bytes, buf: &mut String) {
  if read_u16(basic) != 0 {
     write!(buf,"{}  ",read_u16(basic)).unwrap();
     while parse_opcode(basic, buf) { /* nothing */ }
  }
}

fn main() -> std::io::Result<()> {
    let mut args = args();
    if args.len() != 2 {
       eprintln!("Usage: {} <gwbas file>",
                args.nth(0).unwrap_or(String::from("bascat")));
       std::process::exit(1);
    }
    let fname = args.nth(1).unwrap();
    let buffer = load_buffer(fname)?;
    let mut basic = Bytes { buffer: buffer, index: 1 };
    let stdout = io::stdout();
    let mut handle = stdout.lock();
    let mut line_buf = String::with_capacity(256);
    loop {
       use std::io::Write;
       read_line(&mut basic, &mut line_buf);
       if line_buf.is_empty() { break }
       handle.write_all(line_buf.as_bytes())?;
       line_buf.clear(); 
    }
    Ok(()) 
}
