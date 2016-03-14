package main // import "go.waywardcode.com/bascat"

import (
	"bytes"
	"encoding/binary"
)

// functions to read binary formats from byteDrippers

func fill_array(in byteDripper, bs []byte) (err error) {
	for i := range bs {
		bs[i], err = in.ReadByte()
		if err != nil {
			break
		}
	}
	return
}

func read_int16(in byteDripper) (out int16, err error) {
	var bs [2]byte
	bslice := bs[:]

	err = fill_array(in, bslice)
	binary.Read(bytes.NewBuffer(bslice), binary.LittleEndian, &out)
	return
}

func read_uint16(in byteDripper) (out uint16, err error) {
	var bs [2]byte
	bslice := bs[:]

	err = fill_array(in, bslice)
	binary.Read(bytes.NewBuffer(bslice), binary.LittleEndian, &out)
	return
}

func read_f32(in byteDripper) (out float64, err error) {
	var bs [4]byte
	bslice := bs[:]

	err = fill_array(in, bslice)

	// convert MBF to IEEE
	//  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee
	if bs[3] != 0 {
		sign := bs[2] & 0x80
		exp := bs[3] - 2
		bs[3] = sign | (exp >> 1)
		bs[2] = (exp << 7) | (bs[2] & 0x7F)
		var f32 float32
		binary.Read(bytes.NewBuffer(bslice), binary.LittleEndian, &f32)
		out = float64(f32)
	}
	return
}

func read_f64(in byteDripper) (out float64, err error) {
	var bs [8]byte
	bslice := bs[:]

	err = fill_array(in, bslice)

	// convert MBF to IEEE
	//  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee

	if bs[7] != 0 {
		var iees [8]byte
		islice := iees[:]

		sign := bs[6] & 0x80
		var exp int16 = int16(bs[3]) - 128 - 1 + 1023

		iees[7] = sign | byte(exp>>4)
		iees[6] = byte(exp << 4)

		var idx int
		for idx = 6; idx >= 1; idx-- {
			bs[idx] = (bs[idx] << 1) | (bs[idx-1] >> 7)
			iees[idx] = iees[idx] | (bs[idx] >> 4)
			iees[idx-1] = iees[idx-1] | (bs[idx] << 4)
		}
		bs[0] = bs[0] << 1
		iees[0] = iees[0] | (bs[0] >> 4)

		binary.Read(bytes.NewBuffer(islice), binary.LittleEndian, &out)
	}

	return
}
