/* Copyright (c) 2018 Richard Todd.  
 * This program is GPLv2; see the LICENSE file */

#include<stdint.h>
#include<stdbool.h>
#include<stdio.h>
#include<sys/mman.h>
#include<sys/types.h>
#include<sys/stat.h>
#include<unistd.h>
#include<fcntl.h>

static const char* TOKENS[] = {
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


/* Some support data for the decrypting Iterator */
static const uint8_t KEY11[] = {0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C};
static const uint8_t KEY13[] = {0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A};

/* Decrypt a buffer of data according to the algorithm I found online */
static void decrypt_buffer(uint8_t *buf, size_t len) {
  *buf++ = 0xff;  /* don't need to decrypt again */
  uint8_t *const end = buf + len;
  uint8_t idx11 = 0, idx13 = 0;
  while(buf != end) {
    *buf = ((*buf - (11 - idx11))  ^ 
              (KEY11[idx11]) ^ 
              (KEY13[idx13])) + (13 - idx13);
    ++buf;
    if (++idx11 == 11) idx11 == 0;
    if (++idx13 == 13) idx13 == 0;
  }
}

/* gwbas_data is just a way to track where we are in the buffer.
 * Functions like read_u8 use it, and return 0 past the end.
 * For translating GW-BASIC, returning 0 is safe (the program 
 * will terminate). */
typedef struct {
   uint8_t * buffer;
   size_t len;
   size_t index;
} gwbas_data;

/* Read a u8 from a byte iterator, returning 0u8 on all
 * errors. Zeros are guaranteed to halt the processing,
 * so there is no harm in this behavior in error cases
 * other than we don't report read errors to the user. */
static inline uint8_t read_u8(gwbas_data *const b) {
  return (b->index < b->len) ? b->buffer[b->index++] : 0;
}

/* Peek ahead a byte. */
static inline bool peek_one(gwbas_data *const b, uint8_t val) {
  return (b->index < b->len) && (b->buffer[b->index] == val);
}

/* Peek ahead two bytes. */
static inline bool peek_two(gwbas_data *const b, uint8_t val, uint8_t val2) {
  return (b->index+1 < b->len) && 
         (b->buffer[b->index] == val) && 
         (b->buffer[b->index+1] == val2);
}

/* Read a little-endian i16 from a byte iterator. */
static int16_t read_i16(gwbas_data *const b) {
  int16_t b1 = read_u8(b);
  int16_t b2 = read_u8(b);
  return (b2 << 8) | b1 ;
}

/* Read a little-endian u16 from a byte iterator. */
static uint16_t read_u16(gwbas_data *const b) {
  uint16_t b1 = read_u8(b);
  uint16_t b2 = read_u8(b);
  return (b2 << 8) | b1 ;
}

/* Read a MS MBF-style 32-bit float, and convert it to a modern IEEE float.
 * NB: This function assumes little endian outputs are appropriate. */
static float read_f32(gwbas_data *const b) {
  union { 
	float answer;
    uint8_t bs[4];
  } bytes;	
  bytes.bs[0] = read_u8(b);
  bytes.bs[1] = read_u8(b);
  bytes.bs[2] = read_u8(b);
  bytes.bs[3] = read_u8(b);
  if(bytes.bs[3] == 0) {
     return 0.0;
  } else {
     uint8_t sgn = bytes.bs[2] & 0x80;
     uint8_t exp = (bytes.bs[3] - 2) & 0xff;
     bytes.bs[3] = sgn | (exp >> 1);
     bytes.bs[2] = ((exp << 7) | (bytes.bs[2] & 0x7f)) & 0xff;
     return bytes.answer;
  }
}

/* Read a MS MBF-style 64-bit float, and convert it to a modern IEEE double.
 * NB: This function assumes little endian outputs are appropriate. */
static double read_f64(gwbas_data *const b) {
  union { 
	double answer;
    uint8_t bs[8];
  } bytes;	
  for(int i = 0; i < 8; ++i) { bytes.bs[i] = read_u8(b); }

  if(bytes.bs[7] == 0) { 
    return 0.0;
  } else {
    uint8_t sgn = bytes.bs[6] & 0x80;
    uint16_t exp = ((uint16_t)bytes.bs[3] - 128 - 1 + 1023) & 0xffff;
    bytes.bs[7] = sgn | ((exp >> 4) & 0xff);
    uint8_t left_over = ((exp << 4) & 0xff);
    uint8_t tmp;
    for(int idx = 6; idx > 0; --idx) {
        tmp = ((bytes.bs[idx] << 1) & 0xff) | (bytes.bs[idx-1] >> 7);
        bytes.bs[idx] = left_over | (tmp >> 4);
        left_over = (tmp << 4) & 0xff;
    }
    tmp = (bytes.bs[0] << 1) & 0xff;
    bytes.bs[0] = left_over | (tmp >> 4);
    return bytes.answer;
  }
}

/* Read the first byte of FNAME, and decrypt the file
 * if necessary.  Then, return the unencrypted bytes.  
 * N.B: we just forget about the fd, and never close the file
 * or unmap the memory since this program exits right after
 * finishing anyway. */
static bool load_buffer(gwbas_data *const b, const char *const fname) {
    struct stat sb;
    int fd = open(fname, O_RDONLY);
    if(fd == -1) return false;
    if(fstat(fd, &sb) == -1) return false;

    /* initialize b */
    b->len = sb.st_size;
    b->index = 1;
    b->buffer = mmap(NULL, b->len, PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0);
    if(b->buffer == MAP_FAILED) {
       return false;
    }

    /* decrypt b if necessary, and detect a bad 1st byte */
    switch(b->buffer[0]) {
       case 0xff: break;
       case 0xfe: decrypt_buffer(b->buffer, b->len);
                  break;
       default:   return false;
    }
    return true;
}

/* Display a string for the next opcode, which sometimes requires reading
 * deeper into the file. */
static bool parse_opcode(gwbas_data *const b) {
    uint16_t opcode = read_u8(b);
    if(opcode >= 0xfd) { opcode = (opcode << 8)|read_u8(b); }

    if(opcode == 0x3A && peek_one(b,0xA1)) {
        fputs("ELSE", stdout);
        b->index += 1;
    } else if(opcode == 0x3A && peek_two(b,0x8F,0xD9)) {
        putchar('\'');
        b->index += 2;
    } else if(opcode == 0xB1 && peek_one(b,0xE9)) {
        fputs("WHILE", stdout);
        b->index += 1;
    } else if(opcode >= 0x11 && opcode <= 0x1b) { 
        fputs(TOKENS[opcode - 0x11], stdout);
    } else if(opcode >= 0x20 && opcode <= 0x7e) { 
        putchar(opcode);
    } else if(opcode >= 0x81 && opcode <= 0xf4) { 
        fputs(TOKENS[opcode - 118], stdout);
    } else if(opcode >= 0xfd81 && opcode <= 0xfd8b) { 
        fputs(TOKENS[opcode - 64770], stdout);
    } else if(opcode >= 0xfe81 && opcode <= 0xfea8) { 
        fputs(TOKENS[opcode - 65015], stdout);
    } else if(opcode >= 0xff81 && opcode <= 0xffa5) { 
        fputs(TOKENS[opcode - 65231], stdout);
    } else switch(opcode) {
        case 0x00: putchar('\n'); break;
        case 0x0B: printf("&O%o",read_i16(b)); break;
        case 0x0C: printf("&H%X",read_u16(b)); break;
        case 0x0E: printf("%u",read_u16(b)); break;
        case 0x0F: printf("%u",read_u8(b)); break;
        case 0x1C: printf("%d",read_i16(b)); break;
        case 0x1D: printf("%G",read_f32(b)); break;
        case 0x1F: printf("%G",read_f64(b)); break;
        default: printf("<UNK! {%04X}>", opcode);
   }
   return (opcode != 0);
}

/* Print tokens representing the next line of the GWBAS file.
 * At the EOF just return FALSE */
static bool read_line(gwbas_data *const b) {
  if(read_u16(b) == 0) return false;
  printf("%d  ",read_u16(b));
  while(parse_opcode(b)) { /* nothing */ }
  return true;
}

int main(int argc, char**argv) {
    if(argc != 2) {
       fprintf(stderr, "Usage: %s <gwbas_data file>\n", argv[0]);
       return 1;
    }
    gwbas_data bas;
    if(!load_buffer(&bas, argv[1])) {
       fprintf(stderr, "Error loading buffer for %s\n", argv[1]);
       return 1;
    }
    while(read_line(&bas)) { /* nothing */ }
    return 0;
}
