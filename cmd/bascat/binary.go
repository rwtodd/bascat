package main

import (
	"errors"
	"math/big"
)

// an in-memory source of bytes, which returns 0s instead of EOF errors
type buffer struct {
	bytes []byte
	idx   int
	last2 int
}

func newBuffer(bs []byte) (*buffer, error) {
	b := &buffer{bs, 0, len(bs) - 1}
	switch b.readU8() {
	case 0xff:
		// do nothing
	case 0xfe:
		unprotect(bs)
	default:
		return nil, errors.New("Bad 1st byte of BAS file!")
	}
	return b, nil
}

func (b *buffer) peek(v byte) bool {
	return b.idx < len(b.bytes) && b.bytes[b.idx] == v
}

func (b *buffer) peek2(v1 byte, v2 byte) bool {
	return b.idx < b.last2 && b.bytes[b.idx] == v1 && b.bytes[b.idx+1] == v2
}

func (b *buffer) eof() bool {
	return b.idx >= len(b.bytes)
}

func (b *buffer) skip(amt int) {
	b.idx += amt
}

func (b *buffer) readU8() byte {
	if b.idx < len(b.bytes) {
		v := b.bytes[b.idx]
		b.idx += 1
		return v
	} else {
		return 0
	}
}

func (b *buffer) fillArray(bs []byte) {
	for i := range bs {
		bs[i] = b.readU8()
	}
}

func (b *buffer) readInt16() int16 {
	bs0 := int16(b.readU8())
	bs1 := int16(b.readU8())
	return bs0 | (bs1 << 8)
}
func (b *buffer) readUInt16() uint16 {
	bs0 := uint16(b.readU8())
	bs1 := uint16(b.readU8())
	return bs0 | (bs1 << 8)
}

func (b *buffer) readF32() (out float64) {
	var bs [4]byte
	b.fillArray(bs[:])

	// Read a MBF float, and build a float64 out of it
	if bs[3] != 0 {
		exp := int(bs[3]) - 129
		var result big.Float
		var denom big.Float
		denom.SetPrec(64)
		denom.SetInt64(0x800000)

		result.SetPrec(64)
		result.SetInt64(int64(uint64(bs[0]) |
			(uint64(bs[1]) << 8) |
			(uint64(bs[2]|0x80) << 16)))
		result.Quo(&result, &denom)
		if (bs[2] & 0x80) != 0 {
			result.Neg(&result)
		}
		out, _ = result.SetMantExp(&result, exp).Float64()
	}
	return
}

func (b *buffer) readF64() (out float64) {
	var bs [8]byte
	b.fillArray(bs[:])

	// Read a MBF float, and build a float64 out of it
	if bs[7] != 0 {
		exp := int(bs[7]) - 129
		var denom big.Float
		denom.SetPrec(64)
		denom.SetInt64(0x80000000000000)

		var result big.Float
		result.SetPrec(64)
		result.SetInt64(int64(
			uint64(bs[0]) |
				(uint64(bs[1]) << 8) |
				(uint64(bs[2]) << 16) |
				(uint64(bs[3]) << 24) |
				(uint64(bs[4]) << 32) |
				(uint64(bs[5]) << 40) |
				(uint64(bs[6]|0x80) << 48)))
		result.Quo(&result, &denom)
		if (bs[6] & 0x80) != 0 {
			result.Neg(&result)
		}
		out, _ = result.SetMantExp(&result, exp).Float64()
	}
	return
}
