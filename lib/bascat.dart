import "dart:typed_data";

const _tokens = [
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
  "FN", "SPC(", "NOT", "ERL", "ERR", "STRING\$", "USING", "INSTR",
  "'", "VARPTR", "CSRLIN", "POINT", "OFF", "INKEY\$", "<0xDF!>", "<0xE0!>",
  /* 0xE1 - 0xF0 */
  "<0xE1!>", "<0xE2!>", "<0xE3!>", "<0xE4!>", "<0xE5!>", ">", "=", "<",
  "+", "-", "*", "/", "^", "AND", "OR", "XOR",
  /* 0xF1 - 0xf4 */
  "EQV", "IMP", "MOD", "\\",

  // --- *** BREAK *** ---
  /* 0xFD81 - 0xFD8B */
  "CVI", "CVS", "CVD", "MKI\$", "MKS\$", "MKD\$", "<0xFD87!>", "<0xFD88!>",
  "<0xFD89!>", "<0xFD8A!>", "EXTERR",

  // --- *** BREAK *** ---
  /* 0xFE81 - 0xFE90 */
  "FILES", "FIELD", "SYSTEM", "NAME", "LSET", "RSET", "KILL", "PUT",
  "GET", "RESET", "COMMON", "CHAIN", "DATE\$", "TIME\$", "PAINT", "COM",

  /* 0xFE91 - 0xFEA0 */
  "CIRCLE", "DRAW", "PLAY", "TIMER", "ERDEV", "IOCTL", "CHDIR", "MKDIR",
  "RMDIR", "SHELL", "ENVIRON", "VIEW", "WINDOW", "PMAP", "PALETTE", "LCOPY",

  /* 0xFEA1 - 0xFEA8 */
  "CALLS", "<0xFEA2!>", "<0xFEA3!>", "NOISE", "PCOPY", "TERM", "LOCK", "UNLOCK",

  // --- *** BREAK *** ---
  /* 0xFF81 - 0xFE90 */
  "LEFT\$", "RIGHT\$", "MID\$", "SGN", "INT", "ABS", "SQR", "RND",
  "SIN", "LOG", "EXP", "COS", "TAN", "ATN", "FRE", "INP",

  /* 0xFF91 - 0xFEA0 */
  "POS", "LEN", "STR\$", "VAL", "ASC", "CHR\$", "PEEK", "SPACE\$",
  "OCT\$", "HEX\$", "LPOS", "CINT", "CSNG", "CDBL", "FIX", "PEN",

  /* 0xFFA1 - 0xFFA5 */
  "STICK", "STRIG", "EOF", "LOC", "LOF"
];

class _GwBasDecoder {
  ByteData src; // the data itself
  StringBuffer sb; // a buffer for the output, which we re-use
  int idx; // the current index into `src` based on where we are

  _GwBasDecoder(Uint8List s)
      : src = ByteData.sublistView(s),
        sb = StringBuffer(),
        idx = 1;

  int parseLineNumber() {
    if (src.getUint16(idx, Endian.little) == 0) return -1;
    var lno = src.getUint16(idx + 2, Endian.little);
    idx += 4;
    return lno;
  }

  bool parseOpCode() {
    var opcode = src.getUint8(idx);
    idx = idx + 1;

    if (opcode >= 0xfd) {
      opcode = src.getUint16(idx - 1, Endian.big);
      idx += 1;
    }

    if (opcode == 0x3A &&
        idx < src.lengthInBytes &&
        src.getUint8(idx) == 0xA1) {
      sb.write('ELSE');
      idx += 1;
    } else if (opcode == 0x3A &&
        (idx + 1) < src.lengthInBytes &&
        src.getUint16(idx, Endian.big) == 0x8FD9) {
      sb.write('\'');
      idx += 2;
    } else if (opcode == 0xB1 &&
        idx < src.lengthInBytes &&
        src.getUint8(idx) == 0xE9) {
      sb.write('WHILE');
      idx += 1;
    } else if (opcode >= 0x11 && opcode <= 0x1b) {
      sb.write(_tokens[opcode - 0x11]);
    } else if (opcode >= 0x20 && opcode <= 0x7e) {
      sb.writeCharCode(opcode);
    } else if (opcode >= 0x81 && opcode <= 0xf4) {
      sb.write(_tokens[opcode - 118]);
    } else if (opcode >= 0xfd81 && opcode <= 0xfd8b) {
      sb.write(_tokens[opcode - 64770]);
    } else if (opcode >= 0xfe81 && opcode <= 0xfea8) {
      sb.write(_tokens[opcode - 65015]);
    } else if (opcode >= 0xff81 && opcode <= 0xffa5) {
      sb.write(_tokens[opcode - 65231]);
    } else {
      switch (opcode) {
        case 0x00:
          break;
        case 0x0B:
          sb
            ..write('&O')
            ..write(src.getInt16(idx, Endian.little).toRadixString(8));
          idx += 2;
          break;
        case 0x0C:
          sb
            ..write('&H')
            ..write(src
                .getUint16(idx, Endian.little)
                .toRadixString(16)
                .toUpperCase());
          idx += 2;
          break;
        case 0x0E:
          sb.write(src.getUint16(idx, Endian.little));
          idx += 2;
          break;
        case 0x0F:
          sb.write(src.getUint8(idx));
          idx += 1;
          break;
        case 0x1C:
          sb.write(src.getInt16(idx, Endian.little));
          idx += 2;
          break;
        case 0x1D:
          sb.write(decodeF32().toStringAsPrecision(7));
          break;
        case 0x1F:
          sb.write(decodeF64());
          break;
        default:
          sb.write("<UNK! $opcode>");
      }
    }
    return (opcode != 0);
  }

  String? parseLine() {
    var ln = parseLineNumber();
    if (ln < 0) {
      return null;
    }
    sb
      ..clear()
      ..write(ln)
      ..write("  ");
    while (parseOpCode()) {/* empty */}
    return sb.toString();
  }

  void decrypt() {
    src.setUint8(0, 0xff); // mark it decrypted

    /* Some support data for the decrypting Iterator */
    Uint8List key11 = Uint8List.fromList(
        [0x1E, 0x1D, 0xC4, 0x77, 0x26, 0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C]);
    Uint8List key13 = Uint8List.fromList([
      0xA9,
      0x84,
      0x8D,
      0xCD,
      0x75,
      0x83,
      0x43,
      0x63,
      0x24,
      0x83,
      0x19,
      0xF7,
      0x9A
    ]);

    int idx11 = 0;
    int idx13 = 0;
    int pos = 1;
    while (pos < src.lengthInBytes) {
      src.setUint8(
          pos,
          ((src.getUint8(pos) - (11 - idx11)) ^
                  (key11[idx11]) ^
                  (key13[idx13])) +
              (13 - idx13));
      pos += 1;
      if (++idx11 == 11) idx11 = 0;
      if (++idx13 == 13) idx13 = 0;
    }
  }

  /// Read a MS MBF-style 32-bit float, and convert it to a modern IEEE float.
  double decodeF32() {
    int base = idx;
    idx += 4;

    if (src.getUint8(base + 3) != 0) {
      int sgn = src.getUint8(base + 2) & 0x80;
      int exp = (src.getUint8(base + 3) - 2) & 0xff;
      src.setUint8(base + 3, sgn | (exp >> 1));
      src.setUint8(
          base + 2, ((exp << 7) | (src.getUint8(base + 2) & 0x7f)) & 0xff);
      return src.getFloat32(base, Endian.little);
    }
    return 0.0;
  }

  /// Read a MS MBF-style 64-bit float, and convert it to a modern IEEE double.
  double decodeF64() {
    int base = idx;
    idx += 8;

    if (src.getUint8(base + 7) != 0) {
      int sgn = src.getUint8(base + 6) & 0x80;
      int exp = (src.getUint8(base + 3) - 128 - 1 + 1023) & 0xffff;
      src.setUint8(base + 7, sgn | ((exp >> 4) & 0xff));
      int leftOver = ((exp << 4) & 0xff);
      int tmp = 0;
      for (int pos = 6; pos > 0; --pos) {
        tmp = ((src.getUint8(pos) << 1) & 0xff) | (src.getUint8(pos - 1) >> 7);
        src.setUint8(pos, leftOver | (tmp >> 4));
        leftOver = (tmp << 4) & 0xff;
      }
      tmp = (src.getUint8(base) << 1) & 0xff;
      src.setUint8(base, leftOver | (tmp >> 4));
      return src.getFloat64(base, Endian.little);
    }
    return 0.0;
  }

  void setUp() {
    // make sure the data is decrypted
    switch (src.getUint8(0)) {
      case 0xfe:
        decrypt();
        break;
      case 0xff:
        break;
      default:
        throw FormatException("Bad first byte of file!");
    }
  }
}

Iterable<String> decodeGwBas(Uint8List data) sync* {
  var ds = _GwBasDecoder(data)..setUp();
  try {
    while (true) {
      var nextLine = ds.parseLine();
      if (nextLine == null) break;
      yield nextLine;
    }
  } catch (e) {
    yield "Error during parse: $e";
  }
}
