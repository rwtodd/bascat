/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rwt.bascat;

import java.util.Collections;
import java.util.Map;

/**
 * Just plain data, (opcode,description) pair... with some static helpers
 * to create them.
 * @author richard todd
 */
public final class Token {
     public final int    opcode;
     public final String description;

     private Token(int o, String d) { opcode = o; description = d; }

     public static Token fromOpcode(final int op) { 
         String opname = opcodes.get(op);
         if(opname == null) {
            opname = String.format("<OP:%d>", op);
         }
         return new Token(op, opname ); 
     }
     
     public static Token fromLiteral(final String lit) { return new Token(lit.charAt(0),lit); }
     public static Token fromLiteral(final int ch) { return new Token(ch, Character.toString((char)ch)); }
  
     public static Token fromNumber(long num, int base) {
         String desc;
         switch(base) {
             case 8: desc = String.format("&O%o",num); break;
             case 16: desc = String.format("&H%X",num); break;
             default: desc = Long.toString(num); break;
         }    
         return new Token(-1, desc);
     } 

     public static Token fromFloat(double num) {
         return new Token(-1, String.format("%G",num));
     }
     
   /** opcodes represents the BASIC tokens which are shorthand
    * for keywords. 
    */
  private static final Map<Integer,String> opcodes;
  static {
        Map<Integer,String> ini = new java.util.HashMap<>(200);
       
        ini.put(0x00,"EOL");
	ini.put(0x81,"END");
	ini.put(0x82,"FOR");
	ini.put(0x83,"NEXT");
	ini.put(0x84,"DATA");
	ini.put(0x85,"INPUT");
	ini.put(0x86,"DIM");
	ini.put(0x87,"READ");
	ini.put(0x88,"LET");
	ini.put(0x89,"GOTO");
	ini.put(0x8A,"RUN");
	ini.put(0x8B,"IF");
	ini.put(0x8C,"RESTORE");
	ini.put(0x8D,"GOSUB");
	ini.put(0x8E,"RETURN");
	ini.put(0x8F,"REM");
	ini.put(0x90,"STOP");
	ini.put(0x91,"PRINT");
	ini.put(0x92,"CLEAR");
	ini.put(0x93,"LIST");
	ini.put(0x94,"NEW");
	ini.put(0x95,"ON");
	ini.put(0x96,"WAIT");
	ini.put(0x97,"DEF");
	ini.put(0x98,"POKE");
	ini.put(0x99,"CONT");
	ini.put(0x9C,"OUT");
	ini.put(0x9D,"LPRINT");
	ini.put(0x9E,"LLIST");
	ini.put(0xA0,"WIDTH");
	ini.put(0xA1,"ELSE");
	ini.put(0xA2,"TRON");
	ini.put(0xA3,"TROFF");
	ini.put(0xA4,"SWAP");
	ini.put(0xA5,"ERASE");
	ini.put(0xA6,"EDIT");
	ini.put(0xA7,"ERROR");
	ini.put(0xA8,"RESUME");
	ini.put(0xA9,"DELETE");
	ini.put(0xAA,"AUTO");
	ini.put(0xAB,"RENUM");
	ini.put(0xAC,"DEFSTR");
	ini.put(0xAD,"DEFINT");
	ini.put(0xAE,"DEFSNG");
	ini.put(0xAF,"DEFDBL");
	ini.put(0xB0,"LINE");
	ini.put(0xB1,"WHILE");
	ini.put(0xB2,"WEND");
	ini.put(0xB3,"CALL");
	ini.put(0xB7,"WRITE");
	ini.put(0xB8,"OPTION");
	ini.put(0xB9,"RANDOMIZE");
	ini.put(0xBA,"OPEN");
	ini.put(0xBB,"CLOSE");
	ini.put(0xBC,"LOAD");
	ini.put(0xBD,"MERGE");
	ini.put(0xBE,"SAVE");
	ini.put(0xBF,"COLOR");
	ini.put(0xC0,"CLS");
	ini.put(0xC1,"MOTOR");
	ini.put(0xC2,"BSAVE");
	ini.put(0xC3,"BLOAD");
	ini.put(0xC4,"SOUND");
	ini.put(0xC5,"BEEP");
	ini.put(0xC6,"PSET");
	ini.put(0xC7,"PRESET");
	ini.put(0xC8,"SCREEN");
	ini.put(0xC9,"KEY");
	ini.put(0xCA,"LOCATE");
	ini.put(0xCC,"TO");
	ini.put(0xCD,"THEN");
	ini.put(0xCE,"TAB(");
	ini.put(0xCF,"STEP");
	ini.put(0xD0,"USR");
	ini.put(0xD1,"FN");
	ini.put(0xD2,"SPC(");
	ini.put(0xD3,"NOT");
	ini.put(0xD4,"ERL");
	ini.put(0xD5,"ERR");
	ini.put(0xD6,"STRING$");
	ini.put(0xD7,"USING");
	ini.put(0xD8,"INSTR");
	ini.put(0xD9,"'");
	ini.put(0xDA,"VARPTR");
	ini.put(0xDB,"CSRLIN");
	ini.put(0xDC,"POINT");
	ini.put(0xDD,"OFF");
	ini.put(0xDE,"INKEY$");
	ini.put(0xE6,">");
	ini.put(0xE7,"=");
	ini.put(0xE8,"<");
	ini.put(0xE9,"+");
	ini.put(0xEA,"-");
	ini.put(0xEB,"*");
	ini.put(0xEC,"/");
	ini.put(0xED,"^");
	ini.put(0xEE,"AND");
	ini.put(0xEF,"OR");
	ini.put(0xF0,"XOR");
	ini.put(0xF1,"EQV");
	ini.put(0xF2,"IMP");
	ini.put(0xF3,"MOD");
	ini.put(0xF4,"\\");
	ini.put(0xFD81,"CVI");
	ini.put(0xFD82,"CVS");
	ini.put(0xFD83,"CVD");
	ini.put(0xFD84,"MKI$");
	ini.put(0xFD85,"MKS$");
	ini.put(0xFD86,"MKD$");
	ini.put(0xFD8B,"EXTERR");
	ini.put(0xFE81,"FILES");
	ini.put(0xFE82,"FIELD");
	ini.put(0xFE83,"SYSTEM");
	ini.put(0xFE84,"NAME");
	ini.put(0xFE85,"LSET");
	ini.put(0xFE86,"RSET");
	ini.put(0xFE87,"KILL");
	ini.put(0xFE88,"PUT");
	ini.put(0xFE89,"GET");
	ini.put(0xFE8A,"RESET");
	ini.put(0xFE8B,"COMMON");
	ini.put(0xFE8C,"CHAIN");
	ini.put(0xFE8D,"DATE$");
	ini.put(0xFE8E,"TIME$");
	ini.put(0xFE8F,"PAINT");
	ini.put(0xFE90,"COM");
	ini.put(0xFE91,"CIRCLE");
	ini.put(0xFE92,"DRAW");
	ini.put(0xFE93,"PLAY");
	ini.put(0xFE94,"TIMER");
	ini.put(0xFE95,"ERDEV");
	ini.put(0xFE96,"IOCTL");
	ini.put(0xFE97,"CHDIR");
	ini.put(0xFE98,"MKDIR");
	ini.put(0xFE99,"RMDIR");
	ini.put(0xFE9A,"SHELL");
	ini.put(0xFE9B,"ENVIRON");
	ini.put(0xFE9C,"VIEW");
	ini.put(0xFE9D,"WINDOW");
	ini.put(0xFE9E,"PMAP");
	ini.put(0xFE9F,"PALETTE");
	ini.put(0xFEA0,"LCOPY");
	ini.put(0xFEA1,"CALLS");
	ini.put(0xFEA4,"NOISE");
	ini.put(0xFEA5,"PCOPY");
	ini.put(0xFEA6,"TERM");
	ini.put(0xFEA7,"LOCK");
	ini.put(0xFEA8,"UNLOCK");
	ini.put(0xFF81,"LEFT$");
	ini.put(0xFF82,"RIGHT$");
	ini.put(0xFF83,"MID$");
	ini.put(0xFF84,"SGN");
	ini.put(0xFF85,"INT");
	ini.put(0xFF86,"ABS");
	ini.put(0xFF87,"SQR");
	ini.put(0xFF88,"RND");
	ini.put(0xFF89,"SIN");
	ini.put(0xFF8A,"LOG");
	ini.put(0xFF8B,"EXP");
	ini.put(0xFF8C,"COS");
	ini.put(0xFF8D,"TAN");
	ini.put(0xFF8E,"ATN");
	ini.put(0xFF8F,"FRE");
	ini.put(0xFF90,"INP");
	ini.put(0xFF91,"POS");
	ini.put(0xFF92,"LEN");
	ini.put(0xFF93,"STR$");
	ini.put(0xFF94,"VAL");
	ini.put(0xFF95,"ASC");
	ini.put(0xFF96,"CHR$");
	ini.put(0xFF97,"PEEK");
	ini.put(0xFF98,"SPACE$");
	ini.put(0xFF99,"OCT$");
	ini.put(0xFF9A,"HEX$");
	ini.put(0xFF9B,"LPOS");
	ini.put(0xFF9C,"CINT");
	ini.put(0xFF9D,"CSNG");
	ini.put(0xFF9E,"CDBL");
	ini.put(0xFF9F,"FIX");
	ini.put(0xFFA0,"PEN");
	ini.put(0xFFA1,"STICK");
	ini.put(0xFFA2,"STRIG");
	ini.put(0xFFA3,"EOF");
	ini.put(0xFFA4,"LOC");
	ini.put(0xFFA5,"LOF");
        opcodes = Collections.unmodifiableMap(ini);
    }
}
