package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"
)

const (
	first_byte  = 0xFF // first byte of the file (unencrypted)
	first_bcrpt = 0xFE // first byte of the file (encrypted)
	last_byte   = 0x1A // last byte of the file

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

	unused_const    = 0x10 // if we see these tokens, it's an error
	unused_const2   = 0x1E // if we see these tokens, it's an error
	lpointer_prefix = 0x0D // line pointer, we should NEVER see this
)

type byteDripper interface {
	ReadByte() (c byte, err error)
}

func decoder(in byteDripper) (bd byteDripper, err error) {
	// check the first byte
	b, err := in.ReadByte()
	switch b {
	case first_byte:
		bd = in
	case first_bcrpt:
		bd = in // FIXME make a decryptor
	default:
		fmt.Printf("This file is not a tokenized BAS file!")

	}
	return
}

func read_int16(in byteDripper) (out int16, err error) {
	b1, err := in.ReadByte()
	b2, err2 := in.ReadByte()

	out = (int16(b2) << 8) | int16(b1)
	if err == nil {
		err = err2
	}
	return
}

func read_uint16(in byteDripper) (out uint16, err error) {
	b1, err := in.ReadByte()
	b2, err2 := in.ReadByte()

	out = (uint16(b2) << 8) | uint16(b1)
	if err == nil {
		err = err2
	}
	return
}

func read_lineno(in byteDripper) (uint16, error) {
	// check the "next line" pointer for 0, return EOF if
	// we find it.
	ptr, err := read_uint16(in)
	if ptr == 0 {
		return ptr, io.EOF
	} else if err != nil {
		return ptr, err
	}

	// return the line number...
	return read_uint16(in)
}

func lookup_token(prefix byte, tok byte) string {
	var ans string
	var ok bool
	var lookup map[byte]string

	switch prefix {
	case 0x00:
		lookup = opcodes
	case 0xfd:
		lookup = opcodes_fd
	case 0xfe:
		lookup = opcodes_fe
	case 0xff:
		lookup = opcodes_ff
	}

	ans, ok = lookup[tok]
	if !ok {
		ans = fmt.Sprintf("<TOKEN %02x%02x>", prefix, tok)
	}
	return ans
}

func get_next_tokstr(in byteDripper) (ans string) {
	tok, _ := in.ReadByte()

	// it might represent itself
	if tok >= 0x20 && tok <= 0x7E {
		ans = string(tok)
		return
	}

	// it might be a hard-coded low number
	if tok >= first_ten && tok <= last_ten {
		ans = strconv.Itoa(int(tok) - first_ten)
		return
	}

	// it might require more bytes read
	switch tok {
	case lnumber_prefix:
		lnum, _ := read_uint16(in)
		ans = strconv.Itoa(int(lnum))
	case octal_prefix, hex_prefix, int_prefix_2b:
		snum, _ := read_int16(in)
		ans = strconv.Itoa(int(snum))
	case int_prefix_1b:
		bnum, _ := in.ReadByte()
		ans = strconv.Itoa(int(bnum))
	case float_prefix_4b:
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		ans = "<FNUMBER_4>"
	case float_prefix_8b:
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		in.ReadByte()
		ans = "<FNUMBER_8>"
	case 0xfd, 0xfe, 0xff:
		second, _ := in.ReadByte()
		ans = lookup_token(tok, second)
	case end_of_line:
		// do nothing
	default:
		ans = lookup_token(0, tok)
	}

	return
}

func cat(in byteDripper) {
	for {
		line, err := read_lineno(in)
		if err == io.EOF {
			break
		}
		if err != nil {
			fmt.Printf("\nERROR %s\n", err.Error())
			break
		}

		var tok = strconv.FormatUint(uint64(line), 10)
		var toks = []string{tok}
		tok = "  "
		for tok != "" {
			toks = append(toks, tok)
			tok = get_next_tokstr(in)
		}

		fmt.Printf("%s\n", strings.Join(toks, ""))

	}
}

func main() {
	infl, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Printf("Can't open <%s>!: %s\n", os.Args[1], err.Error())
		return
	}
	defer infl.Close()

	br := bufio.NewReader(infl)
	input, err := decoder(br)
	if err != nil {
		return
	}

	cat(input)

}
