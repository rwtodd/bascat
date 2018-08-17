package main

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

func unprotect(bs []byte) {
	var idx11, idx13 byte
	bs[0] = 0xff // no need to unprotect again
	bs = bs[1:]

	for i, v := range bs {

		v -= 11 - idx11
		v ^= key_11[idx11]
		v ^= key_13[idx13]
		v += 13 - idx13
		bs[i] = v

		idx13++
		if idx13 == 13 {
			idx13 = 0
		}
		idx11++
		if idx11 == 11 {
			idx11 = 0
		}

	}
}
