package org.rwtodd.bascat;

/*
 * This code is released under the GPL. A copy of the licence is in this
 * program's main directory.
 */
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author richa
 */
public class Cmd {

    private static boolean nextToken(final ByteBuffer in, final StringBuilder sb) {
        boolean hasMore = true;
        int nxt = in.get() & 0xff;
        if (nxt >= 0xFD) {
            nxt = (nxt << 8) | (in.get() & 0xff);
        }

        if (nxt == 0x3A || nxt == 0xB1) {
            final int peek2 = in.mark().getShort() & 0xffff;
            final int peek1 = peek2 & 0xff;
            in.reset();

            if(nxt == 0x3A) {
                if (peek1 == 0xA1) { sb.append("ELSE"); in.get(); }
                else if(peek2 == 0xD98F) { sb.append('\''); in.getShort(); }
                else sb.append(':');
            } else {
                sb.append("WHILE");
                if(peek1 == 0xE9) in.get();
            }
        } // a range of ASCII text...
        else if (nxt >= 0x20 && nxt <= 0x7E) {
            sb.append((char) nxt);
        } //  a bunch of ranges that are predefined tokens...
        else if (nxt >= 0x11 && nxt <= 0x1B) {
            sb.append(TOKENS[nxt - 0x11]);
        } else if (nxt >= 0x81 && nxt <= 0xF4) {
            sb.append(TOKENS[nxt - 118]);
        } else if (nxt >= 0xFD81 && nxt <= 0xFD8B) {
            sb.append(TOKENS[nxt - 64770]);
        } else if (nxt >= 0xFE81 && nxt <= 0xFEA8) {
            sb.append(TOKENS[nxt - 65015]);
        } else if (nxt >= 0xFF81 && nxt <= 0xFFA5) {
            sb.append(TOKENS[nxt - 65231]);
        } // a few special cases, mostly for number fomatting
        else {
            switch (nxt) {

                case 0:
                    hasMore = false;
                    break;
                case 0x0B:  // OCTAL
                    sb.append(String.format("&O%o", in.getShort()));
                    break;
                case 0x0C:  // HEX
                    sb.append(String.format("&H%X", in.getShort()));
                    break;
                case 0x0E:  // DECIMAL UNSIGNED SHORT
                    sb.append(in.getShort() & 0xffff);
                    break;
                case 0x0F:  // DECIMAL UNSIGNED BYTE
                    sb.append(in.get() & 0xff);
                    break;
                case 0x1C: // DECIMAL SIGNED SHORT
                    sb.append(in.getShort());
                    break;
                case 0x1D:  // FLOAT 32
                    sb.append(String.format("%g", MBFReader.getMBF32(in)));
                    break;
                case 0x1F: // FLOAT 64
                    sb.append(String.format("%g", MBFReader.getMBF64(in)));
                    break;

                default:
                    sb.append(String.format("<UNK! %x>", nxt));
                    break;
            }
        }

        return hasMore;
    }

    public static void cat(byte[] source, PrintStream ps)
            throws IllegalArgumentException {
        final ByteBuffer in = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN);

        switch (in.get() & 0xff) {
            case 0xFE:
                Unprotector.unprotect(source);
                break;
            case 0xFF:
                break;
            default:
                throw new IllegalArgumentException("Bad 1st byte!");
        }

        final var sb = new StringBuilder(120);
        while (in.hasRemaining()) {
            if (in.getShort() == 0) {
                break; // 0 pointer == EOF
            }
            sb.append(in.getShort() & 0xffff).append("  ");
            while (nextToken(in, sb)) {
                /* do nothing */ }
            ps.println(sb.toString());
            sb.setLength(0);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: bascat filename");
            return;
        }

        try {
            cat(Files.readAllBytes(Paths.get(args[0])), System.out);
        } catch (IOException | BufferUnderflowException | IllegalArgumentException e) {
            System.err.println(e);
        }
    }

    private static final String[] TOKENS = {
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
    };
}
