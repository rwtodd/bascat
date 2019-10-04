(ns org.rwtodd.bascat.core
  (:import (java.nio ByteBuffer ByteOrder BufferUnderflowException)
           (java.nio.file Files Paths))
  (:gen-class))

(def TOKENS
  "Predefined token strings."
  [
   ;; 0x11 - 0x1B 
   "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
   ;; 0x81 - 0x90 
   "END", "FOR", "NEXT", "DATA", "INPUT", "DIM", "READ", "LET",
   "GOTO", "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM", "STOP",
   ;; 0x91 - 0xA0 
   "PRINT", "CLEAR", "LIST", "NEW", "ON", "WAIT", "DEF", "POKE",
   "CONT", "<0x9A!>", "<0x9B!>", "OUT", "LPRINT", "LLIST", "<0x9F!>", "WIDTH",
   ;; 0xA1 - 0xB0 
   "ELSE", "TRON", "TROFF", "SWAP", "ERASE", "EDIT", "ERROR", "RESUME",
   "DELETE", "AUTO", "RENUM", "DEFSTR", "DEFINT", "DEFSNG", "DEFDBL", "LINE",
   ;; 0xB1 - 0xC0 
   "WHILE", "WEND", "CALL", "<0xB4!>", "<0xB5!>", "<0xB6!>", "WRITE", "OPTION",
   "RANDOMIZE", "OPEN", "CLOSE", "LOAD", "MERGE", "SAVE", "COLOR", "CLS",
   ;; 0xC1 - 0xD0 
   "MOTOR", "BSAVE", "BLOAD", "SOUND", "BEEP", "PSET", "PRESET", "SCREEN",
   "KEY", "LOCATE", "<0xCB!>", "TO", "THEN", "TAB(", "STEP", "USR",
   ;; 0xD1 - 0xE0 
   "FN", "SPC(", "NOT", "ERL", "ERR", "STRING$", "USING", "INSTR",
   "'", "VARPTR", "CSRLIN", "POINT", "OFF", "INKEY$", "<0xDF!>", "<0xE0!>",
   ;; 0xE1 - 0xF0 
   "<0xE1!>", "<0xE2!>", "<0xE3!>", "<0xE4!>", "<0xE5!>", ">", "=", "<",
   "+", "-", "*", "/", "^", "AND", "OR", "XOR",
   ;; 0xF1 - 0xf4 
   "EQV", "IMP", "MOD", "\\",
   ;; 0xFD81 - 0xFD8B 
   "CVI", "CVS", "CVD", "MKI$", "MKS$", "MKD$", "<0xFD87!>", "<0xFD88!>",
   "<0xFD89!>", "<0xFD8A!>", "EXTERR",
   ;; 0xFE81 - 0xFE90 
   "FILES", "FIELD", "SYSTEM", "NAME", "LSET", "RSET", "KILL", "PUT",
   "GET", "RESET", "COMMON", "CHAIN", "DATE$", "TIME$", "PAINT", "COM",
   ;; 0xFE91 - 0xFEA0 
   "CIRCLE", "DRAW", "PLAY", "TIMER", "ERDEV", "IOCTL", "CHDIR", "MKDIR",
   "RMDIR", "SHELL", "ENVIRON", "VIEW", "WINDOW", "PMAP", "PALETTE", "LCOPY",
   ;; 0xFEA1 - 0xFEA8 
   "CALLS", "<0xFEA2!>", "<0xFEA3!>", "NOISE", "PCOPY", "TERM", "LOCK", "UNLOCK",
   ;; 0xFF81 - 0xFE90 
   "LEFT$", "RIGHT$", "MID$", "SGN", "INT", "ABS", "SQR", "RND",
   "SIN", "LOG", "EXP", "COS", "TAN", "ATN", "FRE", "INP",
   ;; 0xFF91 - 0xFEA0 
   "POS", "LEN", "STR$", "VAL", "ASC", "CHR$", "PEEK", "SPACE$",
   "OCT$", "HEX$", "LPOS", "CINT", "CSNG", "CDBL", "FIX", "PEN",
   ;; 0xFFA1 - 0xFFA5 
   "STICK", "STRIG", "EOF", "LOC", "LOF"
   ])

(defn- decode-mbf32
  "read an MBF32 floating point number, stored in little-endian format, from
  the provided `ByteBuffer`"
  [buffer]
  (let [bs (.getInt buffer)]
    (if (zero? (bit-and bs 0xff000000))
      0.0
      (let [sign   (if (zero? (bit-and 0x00800000 bs)) 1N -1N)
            exp    (- (bit-and (bit-shift-right bs 24) 0xff) 129)
            scaled (bigint (.shiftLeft (biginteger 1) (Math/abs exp)))
            expt   (if (neg? exp) (/ 1N scaled) scaled)
            scand  (bit-or (bit-and bs 0x007fffff) 0x00800000)]
        (float (* sign (/ scand 0x800000) expt))))))

(defn- decode-mbf64
  "read an MBF64 floating point number, stored in little-endian format, from
  the provided `ByteBuffer`"
  [buffer]
  (let [bs (.getLong buffer)]
    (if (zero? (bit-and bs (bit-shift-left 0xff 56)))
      0.0
      (let [sign   (if (zero? (bit-and 0x0080000000000000 bs)) 1N -1N)
            exp    (- (bit-and (bit-shift-right bs 56) 0xff) 129)
            scaled (bigint (.shiftLeft (biginteger 1) (Math/abs exp)))
            expt   (if (neg? exp) (/ 1N scaled) scaled)
            scand  (bit-or (bit-and bs 0x007fffffffffffff) 0x0080000000000000)]
        (double (* sign (/ scand 0x0080000000000000) expt))))))

(defn- next-token
  "Grabs the next token from the `java.nio.ByteBuffer` and
  formats it into the provided StringBuilder. Returns nil if
  the token was a zero."
  [^ByteBuffer buf ^StringBuilder sb]
  (let [first-byte (bit-and (.get buf) 0xff)
        tok        (if (>= first-byte 0xfd)
                     (bit-or (bit-shift-left first-byte 8)
                             (bit-and (.get buf) 0xff))
                     first-byte)]
    (if (zero? tok)
      nil
      (do 
        (cond
          ;; 3a and b1 signal possible simplifications to the stream
          (or (== tok 0x3a) (== tok 0xb1))
          (let [peek2 (bit-and (.. buf mark getShort) 0xffff)
                peek1 (bit-and peek2 0xff)]
            (.reset buf)
            (if (== tok 0x3a)
              (cond (== peek1 0xa1)   (do (.append sb "ELSE") (.get buf))
                    (== peek2 0xd98f) (do (.append sb \') (.getShort buf))
                    :else             (.append sb \:))
              (do (.append sb "WHILE")
                  (when (== peek1 0xe9) (.get buf)))))

          ;; ascii text range?
          (and (>= tok 0x20) (<= tok 0x7e))
          (.append sb (char tok))

          ;; here are several predefined token ranges
          (and (>= tok 0x11) (<= tok 0x1b)) 
          (.append sb (TOKENS (- tok 0x11)))
          (and (>= tok 0x81) (<= tok 0xf4)) 
          (.append sb (TOKENS (- tok 118)))
          (and (>= tok 0xfd81) (<= tok 0xfd8b)) 
          (.append sb (TOKENS (- tok 64770)))
          (and (>= tok 0xfe81) (<= tok 0xfea8)) 
          (.append sb (TOKENS (- tok 65015)))
          (and (>= tok 0xff81) (<= tok 0xffa5)) 
          (.append sb (TOKENS (- tok 65231)))

          ;; the following need special formatting
          :else
          (case tok
            0x0b (.append sb (format "&O%o" (.getShort buf)))
            0x0c (.append sb (format "&H%X" (.getShort buf)))
            0x0e (.append sb (bit-and (.getShort buf) 0xffff))
            0x0f (.append sb (bit-and (.get buf) 0xff))
            0x1c (.append sb (int (.getShort buf)))
            0x1d (.append sb (format "%g" (decode-mbf32 buf)))
            0x1f (.append sb (format "%g" (decode-mbf64 buf)))
            (.append sb (format "<UNK! %x>" tok))))
        true))))
        
(defn- unprotect!
  "Runs a decryption routine destructively on the source bytes"
  [bs]
  (let [KEY13 [0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83,
               0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A]
        KEY11 [0x1E, 0x1D, 0xC4, 0x77, 0x26,
               0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C]]
    (aset-byte bs 0 -1)
    (dotimes [idx (dec (alength bs))]
      (let [idx13 (mod idx 13)
            idx11 (mod idx 11)
            b     (unchecked-byte (aget bs (inc idx)))]
        (aset-byte bs
                   (inc idx)
                   (unchecked-byte
                    (+ (bit-xor (- b (- 11 idx11))
                                (KEY11 idx11)
                                (KEY13 idx13))
                       (- 13 idx13))))))))
            
(defn bascat
  "Decodes the provided bytes and formats the output to `*out*`."
  [bs]
  (let [buffer (-> bs ByteBuffer/wrap (.order ByteOrder/LITTLE_ENDIAN))
        sb     (StringBuilder. 120)]
    (case (bit-and (.get buffer) 0xff)
      0xFE   (unprotect! bs)
      0xFF   nil
      (throw (IllegalArgumentException. "Bad 1st byte! Not a GWBASIC/BASICA file?")))
    (while (and (.hasRemaining buffer)
                (not (zero? (.getShort buffer))))
      (.. sb
          (append (bit-and (.getShort buffer) 0xffff))
          (append "  "))
      (while (next-token buffer sb))
      (println (.toString sb))
      (.setLength sb 0))))
 
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (zero? (count args))
    (.println *err* "Usage: bascat filename")
    (try
      (bascat (Files/readAllBytes (Paths/get (nth args 0)
                                             (into-array String []))))
      (catch BufferUnderflowException e (.println *err* "Unexpected end of the file!"))
      (catch Exception e (println (str "caught exception: " (.getMessage e)))))))

