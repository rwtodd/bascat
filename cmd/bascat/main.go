package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
)

// nextToken decodes the next token from the input.
func nextToken(b *buffer, sb *strings.Builder) (hasMore bool) {
	tok := int(b.readU8())
	if tok >= 0xfd {
		tok = (tok << 8) | int(b.readU8())
	}
	hasMore = true

	switch {
	// it might be one of three special patterns
	case tok == 0x3A && b.peek(0xA1):
		sb.WriteString("ELSE")
		b.skip(1)
	case tok == 0x3A && b.peek2(0x8F, 0xD9):
		sb.WriteString("'")
		b.skip(2)
	case tok == 0xB1 && b.peek(0xE9):
		sb.WriteString("WHILE")
		b.skip(1)

		// it might be the end of line
	case tok == 0:
		hasMore = false

		// it could be a formatted number
	case tok == 0x0B:
		sb.WriteString("&O" + strconv.FormatInt(int64(b.readInt16()), 8))
	case tok == 0x0C:
		sb.WriteString("&H" + strconv.FormatInt(int64(b.readInt16()), 16))
	case tok == 0x0E:
		sb.WriteString(strconv.Itoa(int(b.readUInt16())))
	case tok == 0x0F:
		sb.WriteString(strconv.Itoa(int(b.readU8())))
	case tok == 0x1C:
		sb.WriteString(strconv.Itoa(int(b.readInt16())))
	case tok == 0x1D:
		sb.WriteString(strconv.FormatFloat(b.readF32(), 'G', -1, 32))
	case tok == 0x1F:
		sb.WriteString(strconv.FormatFloat(b.readF64(), 'G', -1, 64))

		// it might represent itself
	case tok >= 0x20 && tok <= 0x7E:
		sb.WriteByte(byte(tok))

	// it might be a predefined token
	case tok >= 0x11 && tok <= 0x1B:
		sb.WriteString(tokens[tok-0x11])
	case tok >= 0x81 && tok <= 0xF4:
		sb.WriteString(tokens[tok-118])
	case tok >= 0xFD81 && tok <= 0xFD8B:
		sb.WriteString(tokens[tok-64770])
	case tok >= 0xFE81 && tok <= 0xFEA8:
		sb.WriteString(tokens[tok-65015])
	case tok >= 0xFF81 && tok <= 0xFFA5:
		sb.WriteString(tokens[tok-65231])

	// or... unrecognized!
	default:
		sb.WriteString("<UNK 0x")
		sb.WriteString(strconv.FormatInt(int64(tok), 16))
		sb.WriteString("!>")
	}

	return
}

// cat is the high-level driver of the program (named after the
// UNIX tool.  It pulls in a line of tokens at a time, and sends
// them to be output.
func cat(b *buffer) {
	var sb strings.Builder
	for !b.eof() {
		if b.readUInt16() == 0 {
			break
		}
		sb.WriteString(strconv.Itoa(int(b.readUInt16())))
		sb.WriteString("  ")
		for nextToken(b, &sb) { /* empty */
		}
		fmt.Println(sb.String())
		sb.Reset()
	}
}

func main() {

	var bytes []byte
	var err error

	switch len(os.Args) {
	case 1:
		bytes, err = ioutil.ReadAll(os.Stdin)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Can't read stdin!: %s\n", err.Error())
			os.Exit(1)
		}
	case 2:
		bytes, err = ioutil.ReadFile(os.Args[1])
		if err != nil {
			fmt.Fprintf(os.Stderr, "Can't read <%s>!: %s\n", os.Args[1], err.Error())
			os.Exit(1)
		}
	default:
		fmt.Fprintf(os.Stderr, "Usage: bascat [file]\n")
		os.Exit(2)
	}

	buff, err := newBuffer(bytes)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}

	cat(buff)
}
