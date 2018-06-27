(in-package "RT-BASCAT")

;; These encryption keys are used in GET-READER below on certain files.
(defconstant +key13+ #(#xA9 #x84 #x8D #xCD #x75 #x83 #x43 #x63 #x24 #x83 #x19 #xF7 #x9AA))
(defconstant +key11+ #(#x1E #x1D #xC4 #x77 #x26 #x97 #xE0 #x74 #x59 #x88 #x7C))

(defun get-reader (stream)
  "generate a reading function for the stream, which
   might be encrypted"
  (let ((first (read-byte stream nil 0)))
    (cond ((= #xff first)  ;; plain byte reader...
	   #'(lambda () (read-byte stream nil 0)))
	  
	  ((= #xfe first)  ;; decrypting reader...
	   (let ((idx11 0)
		 (idx13 0))
	     #'(lambda ()
		 (let ((b (read-byte stream nil 'end)))
		   (if (eq b 'end)
		       0
		     (prog1
			 (+ (logxor (- b (- 11 idx11))
				    (aref +key11+ idx11)
				    (aref +key13+ idx13))
			    (- 13 idx13))
		       (setf idx11 (mod (1+ idx11) 11)
			     idx13 (mod (1+ idx13) 13))))))))
	  
	  (t (error "Not a tokenized BAS file!")))))

(defun read-2-bytes (rdr) (values (funcall rdr) (funcall rdr)))
(defun read-4-bytes (rdr) (values (funcall rdr) (funcall rdr) (funcall rdr) (funcall rdr)))
(defun read-8-bytes (rdr) (values (funcall rdr) (funcall rdr) (funcall rdr) (funcall rdr)
				  (funcall rdr) (funcall rdr) (funcall rdr) (funcall rdr)))

(defun read-u16 (rdr)
  "read an unsigned 16-bit value from RDR (little-endian)"
  (multiple-value-bind (a b) (read-2-bytes rdr)
    (logior (ash b 8) a)))

(defun sign-extend-16 (n)
  "sign-extend a 16-bit 2's complement number"
  (if (= (logand #x8000 n) #x8000)
      (- 0 (logxor #xffff n) 1)
    n))

(defun read-s16 (rdr)
  "read a signed 16-bit value from RDR (little-endian)"
  (sign-extend-16 (read-u16 rdr)))

(defun read-mbf-32 (rdr)
  "read a MBF-32 floating point number"
  (multiple-value-bind (a b c d) (read-4-bytes rdr)
    (let ((sign  (if (zerop (logand #x80 c)) 1 -1))
	  (exp   (- d 129))
	  (scand (logior (ash (logand #x7f c) 16)
			 (ash b 8)
			 a)))
      (if (= -129 exp)
	  (float 0)
	(float (* sign (1+ (/ scand #x800000)) (expt 2 exp)))))))

(defun read-mbf-64 (rdr)
  "read a MBF-64 floating point number"
  (multiple-value-bind (a b c d e f g h) (read-8-bytes rdr)
    (let ((sign  (if (zerop (logand #x80 g)) 1 -1))
	  (exp   (- h 129))
	  (scand (logior (ash (logand #x7f g) 48)
			 (ash f 40)
			 (ash e 32)
			 (ash d 24)
			 (ash c 16)
			 (ash b 8)
			 a)))
      (if (= -129 exp)
	  (float 0 1.0d0)
	(float (* sign (1+ (/ scand #x80000000000000)) (expt 2 exp)) 1.0d0)))))

(defconstant +opcodes+
  (macrolet ((entries (&rest ents)
		      `(setf ,@(loop for (k v) on ents by #'cddr nconc `((gethash ,k tbl) ,v)))))    
    (let ((tbl (make-hash-table)))
      (entries
       #x00  "EOL"
       #x0B  #'(lambda (rdr) (cons -1 (format nil "&O~o" (read-u16 rdr))))
       #x0C  #'(lambda (rdr) (cons -1 (format nil "&H~x" (read-s16 rdr))))
       #x0E  #'(lambda (rdr) (cons -1 (write-to-string (read-u16 rdr))))
       #x0F  #'(lambda (rdr) (cons -1 (write-to-string (funcall rdr))))
       #x11  "0"
       #x12  "1"
       #x13  "2"
       #x14  "3"
       #x15  "4"
       #x16  "5"
       #x17  "6"
       #x18  "7"
       #x19  "8"
       #x1A  "9"
       #x1B  "10"
       #x1C  #'(lambda (rdr)  (cons -1 (write-to-string (read-s16 rdr))))
       #x1D  #'(lambda (rdr) (cons -1 (format nil "~f" (read-mbf-32 rdr))))
       #x1F  #'(lambda (rdr) (cons -1 (format nil "~f" (read-mbf-64 rdr))))
       #x81  "END"
       #x82  "FOR"
       #x83  "NEXT"
       #x84  "DATA"
       #x85  "INPUT"
       #x86  "DIM"
       #x87  "READ"
       #x88  "LET"
       #x89  "GOTO"
       #x8A  "RUN"
       #x8B  "IF"
       #x8C  "RESTORE"
       #x8D  "GOSUB"
       #x8E  "RETURN"
       #x8F  "REM"
       #x90  "STOP"
       #x91  "PRINT"
       #x92  "CLEAR"
       #x93  "LIST"
       #x94  "NEW"
       #x95  "ON"
       #x96  "WAIT"
       #x97  "DEF"
       #x98  "POKE"
       #x99  "CONT"
       #x9C  "OUT"
       #x9D  "LPRINT"
       #x9E  "LLIST"
       #xA0  "WIDTH"
       #xA1  "ELSE"
       #xA2  "TRON"
       #xA3  "TROFF"
       #xA4  "SWAP"
       #xA5  "ERASE"
       #xA6  "EDIT"
       #xA7  "ERROR"
       #xA8  "RESUME"
       #xA9  "DELETE"
       #xAA  "AUTO"
       #xAB  "RENUM"
       #xAC  "DEFSTR"
       #xAD  "DEFINT"
       #xAE  "DEFSNG"
       #xAF  "DEFDBL"
       #xB0  "LINE"
       #xB1  "WHILE"
       #xB2  "WEND"
       #xB3  "CALL"
       #xB7  "WRITE"
       #xB8  "OPTION"
       #xB9  "RANDOMIZE"
       #xBA  "OPEN"
       #xBB  "CLOSE"
       #xBC  "LOAD"
       #xBD  "MERGE"
       #xBE  "SAVE"
       #xBF  "COLOR"
       #xC0  "CLS"
       #xC1  "MOTOR"
       #xC2  "BSAVE"
       #xC3  "BLOAD"
       #xC4  "SOUND"
       #xC5  "BEEP"
       #xC6  "PSET"
       #xC7  "PRESET"
       #xC8  "SCREEN"
       #xC9  "KEY"
       #xCA  "LOCATE"
       #xCC  "TO"
       #xCD  "THEN"
       #xCE  "TAB("
       #xCF  "STEP"
       #xD0  "USR"
       #xD1  "FN"
       #xD2  "SPC("
       #xD3  "NOT"
       #xD4  "ERL"
       #xD5  "ERR"
       #xD6  "STRING$"
       #xD7  "USING"
       #xD8  "INSTR"
       #xD9  "'"
       #xDA  "VARPTR"
       #xDB  "CSRLIN"
       #xDC  "POINT"
       #xDD  "OFF"
       #xDE  "INKEY$"
       #xE6  ">"
       #xE7  "="
       #xE8  "<"
       #xE9  "+"
       #xEA  "-"
       #xEB  "*"
       #xEC  "/"
       #xED  "^"
       #xEE  "AND"
       #xEF  "OR"
       #xF0  "XOR"
       #xF1  "EQV"
       #xF2  "IMP"
       #xF3  "MOD"
       #xF4  "\\"
       #xFD81  "CVI"
       #xFD82  "CVS"
       #xFD83  "CVD"
       #xFD84  "MKI$"
       #xFD85  "MKS$"
       #xFD86  "MKD$"
       #xFD8B  "EXTERR"
       #xFE81  "FILES"
       #xFE82  "FIELD"
       #xFE83  "SYSTEM"
       #xFE84  "NAME"
       #xFE85  "LSET"
       #xFE86  "RSET"
       #xFE87  "KILL"
       #xFE88  "PUT"
       #xFE89  "GET"
       #xFE8A  "RESET"
       #xFE8B  "COMMON"
       #xFE8C  "CHAIN"
       #xFE8D  "DATE$"
       #xFE8E  "TIME$"
       #xFE8F  "PAINT"
       #xFE90  "COM"
       #xFE91  "CIRCLE"
       #xFE92  "DRAW"
       #xFE93  "PLAY"
       #xFE94  "TIMER"
       #xFE95  "ERDEV"
       #xFE96  "IOCTL"
       #xFE97  "CHDIR"
       #xFE98  "MKDIR"
       #xFE99  "RMDIR"
       #xFE9A  "SHELL"
       #xFE9B  "ENVIRON"
       #xFE9C  "VIEW"
       #xFE9D  "WINDOW"
       #xFE9E  "PMAP"
       #xFE9F  "PALETTE"
       #xFEA0  "LCOPY"
       #xFEA1  "CALLS"
       #xFEA4  "NOISE"
       #xFEA5  "PCOPY"
       #xFEA6  "TERM"
       #xFEA7  "LOCK"
       #xFEA8  "UNLOCK"
       #xFF81  "LEFT$"
       #xFF82  "RIGHT$"
       #xFF83  "MID$"
       #xFF84  "SGN"
       #xFF85  "INT"
       #xFF86  "ABS"
       #xFF87  "SQR"
       #xFF88  "RND"
       #xFF89  "SIN"
       #xFF8A  "LOG"
       #xFF8B  "EXP"
       #xFF8C  "COS"
       #xFF8D  "TAN"
       #xFF8E  "ATN"
       #xFF8F  "FRE"
       #xFF90  "INP"
       #xFF91  "POS"
       #xFF92  "LEN"
       #xFF93  "STR$"
       #xFF94  "VAL"
       #xFF95  "ASC"
       #xFF96  "CHR$"
       #xFF97  "PEEK"
       #xFF98  "SPACE$"
       #xFF99  "OCT$"
       #xFF9A  "HEX$"
       #xFF9B  "LPOS"
       #xFF9C  "CINT"
       #xFF9D  "CSNG"
       #xFF9E  "CDBL"
       #xFF9F  "FIX"
       #xFFA0  "PEN"
       #xFFA1  "STICK"
       #xFFA2  "STRIG"
       #xFFA3  "EOF"
       #xFFA4  "LOC"
       #xFFA5  "LOF")
      tbl)))

(defun read-token (rdr)
  "read the next token from the token stream"
  (flet ((opcode-lookup (n)
			(let ((val (gethash n +opcodes+)))
			  (typecase val
			    (string (cons n val))
			    (function (funcall val rdr))
			    (t (cons n (format nil "<INVALID ~a>" n)))))))
    (let ((opcode (funcall rdr)))
      (cond ((<= #x20 opcode #x7e) (cons opcode (code-char opcode)))
	    ((<= #xfd opcode #xff) (opcode-lookup (logior (ash opcode 8)
							  (funcall rdr))))
	    (t (opcode-lookup opcode))))))


(defun display-gwbas (rdr stream)
  "read all the lines of the file, and format them to STREAM"
  (flet ((parse-line ()
		     (if (zerop (read-u16 rdr))
			 nil
		       (cons (cons -1 (format nil "~d  " (read-u16 rdr)))
			     (loop :for tk = (read-token rdr)
				   :while (not (zerop (car tk)))
				   :collect tk))))
	 (format-line (line)
		      (flet ((looking-at (key)
					 (loop :for kval :in key
					       :and lval = line :then (cdr lval)
					       :always (equal kval (caar lval)))))
			(do ()
			    ((null line) (format stream "~%"))
			  (cond ((looking-at '(#x3A #xA1)) (setf line (cdr line)))
				((looking-at '(#x3A #x8F #xD9)) (setf line (cddr line)))
				((looking-at '(#xB1 #xE9)) (setf (cadr line) (car line)
								 line (cdr line))))
			  (princ (cdar line) stream)
			  (setf line (cdr line))))))
    (loop :for l = (parse-line) :while l :do (format-line l))))
   
(defun bascat (fname &optional (stream *standard-output*))
  "Parse a tokenized BASIC file given by FNAME, formatting output to STREAM."
  (with-open-file (inp fname
		       :direction :input
		       :element-type '(unsigned-byte 8))
		  (display-gwbas (get-reader inp) stream)))
