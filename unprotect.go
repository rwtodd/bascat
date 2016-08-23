package main

import "io"

// a decrypter for protected BAS files.
// I found the algorithm in a python program ("PC-BASIC"),
//    (  http://sourceforge.net/p/pcbasic/wiki/Home/  )
// ... but the algorithm was published in:
// The Cryptogram computer supplement #19, American Cryptogram Association, Summer 1994

// Basically there is a 13-byte and an 11-byte key, which are BOTH applied
// in a cycle across the bytes of the input.  Also, a reversed 11-index is subtracted
// from the byte, while a reversed 13-index is added to the byte. By 'reversed', I
// mean that as the 11-index goes from 0 to 10, the reversed index goes from 11 to 1.

var key_13 = [...]byte{0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83, 0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A}
var key_11 = [...]byte{0x1E, 0x1D, 0xC4, 0x77, 0x26, 0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C}

type unprotector struct {
	src          io.ByteReader
	idx13, idx11 int
}

func (u *unprotector) ReadByte() (byte, error) {
	ans, err := u.src.ReadByte()
	ans -= 11 - byte(u.idx11)
	ans ^= key_11[u.idx11]
	ans ^= key_13[u.idx13]
	ans += 13 - byte(u.idx13)

	u.idx13++
	if u.idx13 == 13 {
		u.idx13 = 0
	}
	u.idx11++
	if u.idx11 == 11 {
		u.idx11 = 0
	}

	return ans, err
}
