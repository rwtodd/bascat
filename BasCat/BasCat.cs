using System;
using System.Text;

namespace BasCat
{
    internal sealed class BasCat
    {
        private readonly BinReader rdr;

        internal BasCat(byte[] buf)
        {
            rdr = new BinReader(buf);

            switch (rdr.ReadByte())
            {
                case 0xff:
                    break;
                case 0xfe:
                    Unprotector.Decode(buf);
                    break;
                default:
                    throw new NotSupportedException("Not a recognizeable GW-BASIC file!");
            }
        }

        private bool PrintToken(System.IO.TextWriter tw)
        {
            Int32 b = rdr.ReadByte();
            if (b >= 0xfd) b = (b << 8) | (rdr.ReadByte());

            var hasMore = true;
            switch (b)
            {
                case 0x3A when rdr.Peek(0xA1):
                    tw.Write("ELSE"); rdr.Skip(1); break;
                case 0x3A when rdr.Peek(0x8F,0xD9):
                    tw.Write('\''); rdr.Skip(2); break;
                case 0xB1 when rdr.Peek(0xE9):
                    tw.Write("WHILE"); rdr.Skip(1); break;
                case 0x00: tw.WriteLine(); hasMore = false; break;
                case 0x0B: tw.Write("&O{0}", Convert.ToString(rdr.ReadS16(), 8)); break;
                case 0x0C: tw.Write("&H{0:x}", rdr.ReadS16()); break;
                case 0x0E: tw.Write(rdr.ReadU16()); break;
                case 0x0F: tw.Write(rdr.ReadByte()); break;
                case var x when (x >= 0x20 && x <= 0x7E):
                    tw.Write((char)x); break;
                case var x when (x >= 0x11 && x <= 0x1B):
                    tw.Write(Tokens[x - 0x11]); break;
                case 0x1C: tw.Write(rdr.ReadS16()); break;
                case 0x1D: tw.Write("{0:G}",rdr.ReadMBF32()); break;
                case 0x1F: tw.Write("{0:G}",rdr.ReadMBF64()); break;
                case var x when (x >= 0x81 && x <= 0xF4):
                    tw.Write(Tokens[x - 118]); break;
                case var x when (x >= 0xFD81 && x <= 0xFD8B):
                    tw.Write(Tokens[x - 64770]); break;
                case var x when (x >= 0xFE81 && x <= 0xFEA8):
                    tw.Write(Tokens[x - 65015]); break;
                case var x when (x >= 0xFF81 && x <= 0xFFA5):
                    tw.Write(Tokens[x - 65231]); break;
                default:
                    tw.Write("<UNK {0}!>", b);
                    break;
            }
            return hasMore;
        }

        internal void PrintAllLines(System.IO.TextWriter tw)
        {
            var sb = new StringBuilder(120);
            var sw = new System.IO.StringWriter(sb);
            while (!rdr.EOF)
            {
                if (rdr.ReadU16() == 0) break;  // 0 pointer == EOF
                sw.Write(rdr.ReadU16());
                sw.Write("  ");
                while(PrintToken(sw)) {  /* nothing */ }
                tw.Write(sw.ToString());
                sb.Clear();
            }
        }

        private static readonly String[] Tokens =
        {
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
        };

    }
}
