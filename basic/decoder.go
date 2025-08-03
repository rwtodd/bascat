package basic

import (
	"iter"
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

// DecodeLines returns an iterator over the line numbers (uint16)
// and lines (string) of the BASIC program given in the `bs` byte array.
// If the byte array does not look right, it returns an error instead.
func DecodeLines(bs []byte) (iter.Seq2[uint16,string], error) {
	b, err := newBuffer(bs)
	if err != nil {
		return nil, err
	}

	return func(yield func(uint16, string) bool) {
		var sb strings.Builder
		for !b.eof() {
			if b.readUInt16() == 0 {
				break
			}

			lineno := b.readUInt16()
			for nextToken(b, &sb) { /* empty */
			}
			if !yield(lineno, sb.String()) {
				return
			}
			sb.Reset()
		}
	}, nil
}
