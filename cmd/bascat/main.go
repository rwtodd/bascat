package main

import (
	"bufio"
	"fmt"
	"io"
	"os"

	"github.com/rwtodd/Go.AppUtil/errs"
)

const (
	first_byte  = 0xFF // first byte of the file (unencrypted)
	first_bcrpt = 0xFE // first byte of the file (encrypted)

	end_of_program = 0 // indicates there are no more lines
	end_of_line    = 0 // indicates the end of a line

	lnumber_prefix = 0x0E // line number prefix, unsigned

	octal_prefix  = 0x0B // comes before an octal constant, signed
	hex_prefix    = 0x0C // comes before a hex constant, signed
	first_ten     = 0x11 // 0x11 through 0x1B are the constants 0 to 10
	last_ten      = 0x1B // 0x11 through 0x1B are the constants 0 to 10
	int_prefix_1b = 0x0F // one-byte int prefix, 11 - 255
	int_prefix_2b = 0x1C // two-byte int prefix, signed

	float_prefix_4b = 0x1D // four-byte int prefix
	float_prefix_8b = 0x1F // eight-byte float prefix
)

// decoder decides whether to use the 'unprotect' decryption or not.
func decoder(in io.ByteReader) (bd io.ByteReader, err error) {
	// check the first byte
	b, err := in.ReadByte()
	switch b {
	case first_byte:
		bd = in
	case first_bcrpt:
		bd = &unprotector{src: in}
	default:
		err = errs.First("Decoding",
			err,
			fmt.Errorf("This file is not a tokenized BAS file! First byte: <%02x>", b))
	}
	return
}

// readLineNum reads the next line number from the stream. It
// detects EOF via a "next line pointer" of 0.
func readLineNum(in io.ByteReader) (uint16, error) {
	// check the "next line" pointer for 0, return EOF if
	// we find it.
	ptr, err := readUint16(in)
	if ptr == 0 {
		return ptr, io.EOF
	} else if err != nil {
		return ptr, err
	}

	// return the line number...
	return readUint16(in)
}

// nextToken decodes the next token from the input.
func nextToken(in io.ByteReader) (ans *token) {
	tok, _ := in.ReadByte()

	// it might represent itself
	if tok >= 0x20 && tok <= 0x7E {
		ans = literalToken(string(tok))
		return
	}

	// it might be a hard-coded low number
	if tok >= first_ten && tok <= last_ten {
		ans = numToken((int64(tok) - first_ten), 10)
		return
	}

	// it might require more bytes read
	switch tok {
	case lnumber_prefix:
		lnum, _ := readUint16(in)
		ans = numToken(int64(lnum), 10)
	case octal_prefix:
		snum, _ := readInt16(in)
		ans = numToken(int64(snum), 8)
	case hex_prefix:
		snum, _ := readInt16(in)
		ans = numToken(int64(snum), 16)
	case int_prefix_2b:
		snum, _ := readInt16(in)
		ans = numToken(int64(snum), 10)
	case int_prefix_1b:
		bnum, _ := in.ReadByte()
		ans = numToken(int64(bnum), 10)
	case float_prefix_4b:
		fnum, _ := readF32(in)
		ans = fnumToken(fnum, 32)
	case float_prefix_8b:
		fnum, _ := readF64(in)
		ans = fnumToken(fnum, 64)
	case 0xfd, 0xfe, 0xff:
		second, _ := in.ReadByte()
		ans = opcodeToken((uint16(tok) << 8) | uint16(second))
	case end_of_line:
		ans = nil
	default:
		ans = opcodeToken(uint16(tok))
	}

	return
}

// outputFiltered filters the tokens based on a few patterns:
// 3A A1     --> A1   ":ELSE"  --> "ELSE"
// 3A 8F D9  --> D9   ":REM'"  --> "'"
// B1 E9     --> B1   "WHILE+" --> "WHILE"
func outputFiltered(toks []*token) {
	idx, ln := 0, len(toks)
	lookingAt := func(tgt ...uint16) bool {
		if len(tgt) > (ln - idx) {
			return false
		}
		for i, v := range tgt {
			if toks[idx+i].opcode != v {
				return false
			}
		}
		return true
	}

	for idx < ln {
		if lookingAt(0x3A, 0xA1) {
			idx++
		} else if lookingAt(0x3A, 0x8F, 0xD9) {
			idx += 2
		} else if lookingAt(0xB1, 0xE9) {
			toks[idx+1] = toks[idx]
			idx++
		}
		fmt.Print(toks[idx].str)
		idx++
	}

	fmt.Println("")
}

// cat is the high-level driver of the program (named after the
// UNIX tool.  It pulls in a line of tokens at a time, and sends
// them to be output.
func cat(in io.ByteReader) {
	for {
		line, err := readLineNum(in)
		if err == io.EOF {
			break
		}
		if err != nil {
			fmt.Printf("\nERROR %s\n", err.Error())
			os.Exit(1)
		}

		var tok *token = numToken(int64(line), 10)
		var toks = append(make([]*token, 0, 20), tok)
		tok = literalToken("  ")
		for tok != nil {
			toks = append(toks, tok)
			tok = nextToken(in)
		}

		outputFiltered(toks)
	}
}

func main() {
	var infl *os.File

	switch len(os.Args) {
	case 1:
		infl = os.Stdin
	case 2:
		var err error
		infl, err = os.Open(os.Args[1])
		if err != nil {
			fmt.Printf("Can't open <%s>!: %s\n", os.Args[1], err.Error())
			os.Exit(1)
		}
		defer infl.Close()
	default:
		fmt.Fprintf(os.Stderr, "Usage: bascat [file]\n")
		os.Exit(2)
	}

	br := bufio.NewReader(infl)
	input, err := decoder(br)
	if err != nil {
		fmt.Println(err)
		return
	}

	cat(input)
}
