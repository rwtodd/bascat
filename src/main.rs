use std::env::args;

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

#[derive(Debug)]
struct Token  {
  num: u16,
  desc: String,
}

impl Token {
  fn new(num : u16) -> Token {
    match num {
      0x00 => Token { num: 0, desc: String::from("EOF") },
      /* 0x0B => (lambda rdr: _Token.from_number(rdr.read_16(), 8)),
      0x0C => (lambda rdr: _Token.from_number(rdr.read_16(), 16)),
      0x0E => (lambda rdr: _Token.from_number(rdr.read_u16(), 10)),
      0x0F => (lambda rdr: _Token.from_number(rdr.read_u8(), 10)),
      0x1C => (lambda rdr: _Token.from_number(rdr.read_16(), 10)),
      0x1D => (lambda rdr: _Token.from_float(rdr.read_f32())),
      0x1F => (lambda rdr: _Token.from_float(rdr.read_f64())),  */
      0x11 ... 0x1B => Token { num: num, desc: String::from(TOKENS[(num - 0x11) as usize]) },
      0x81 ... 0xF4 => Token { num: num, desc: String::from(TOKENS[(num - 118) as usize]) },
      0xFD81 ... 0xFD8B => Token { num: num, desc: String::from(TOKENS[(num - 64770) as usize]) },
      0xFE81 ... 0xFEA8 => Token { num: num, desc: String::from(TOKENS[(num - 65015) as usize]) },
      0xFF81 ... 0xFFA5 => Token { num: num, desc: String::from(TOKENS[(num - 65231) as usize]) },
      _ => Token { num: 1, desc: String::from("UNK!") },
   }
 }
}

fn main() {
    let which = args().nth(1).unwrap_or_else(|| String::from("0"))
                      .parse::<u16>().unwrap();
    println!("Hello, world!: {:?}", Token::new(which))
}
