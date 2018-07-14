package main

import (
	"io"
	"math/big"
)

// functions to read binary formats from byteDrippers

func fillArray(in io.ByteReader, bs []byte) (err error) {
	for i := range bs {
		bs[i], err = in.ReadByte()
		if err != nil {
			break
		}
	}
	return
}

func readInt16(in io.ByteReader) (out int16, err error) {
	var bs [2]byte

	err = fillArray(in, bs[:])
	out = int16(bs[0]) | (int16(bs[1]) << 8)
	return
}

func readUint16(in io.ByteReader) (out uint16, err error) {
	var bs [2]byte

	err = fillArray(in, bs[:])
	out = uint16(bs[0]) | (uint16(bs[1]) << 8)
	return
}

func readF32(in io.ByteReader) (out float64, err error) {
	var bs [4]byte
	err = fillArray(in, bs[:])

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

func readF64(in io.ByteReader) (out float64, err error) {
	var bs [8]byte

	err = fillArray(in, bs[:])
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
