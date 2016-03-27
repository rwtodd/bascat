// copyright 2016 Richard Todd.
// License is GPL... see the LICENSE file in the repository.

package rwt.bascat

// a decrypter for protected BAS files.
// I found the algorithm in a python program ("PC-BASIC"),
//    (  http://sourceforge.net/p/pcbasic/wiki/Home/  )
// ... but the algorithm was published in:
// The Cryptogram computer supplement #19, American Cryptogram Association, Summer 1994

// Basically there is a 13-byte and an 11-byte key, which are BOTH applied
// in a cycle across the bytes of the input.  Also, a reversed 11-index is subtracted
// from the byte, while a reversed 13-index is added to the byte. By 'reversed', I
// mean that as the 11-index goes from 0 to 10, the reversed index goes from 11 to 1.

class Unprotector(ins: java.io.InputStream) {
  private val key13 = Array(0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83, 
                            0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A)
  private val key11 = Array(0x1E, 0x1D, 0xC4, 0x77, 0x26, 
                            0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C)

  private var idx13 = 0
  private var idx11 = 0

  def read():Int = {
      var ans = ins.read()      
      if (ans == -1) throw new java.io.EOFException("EOF")
      ans -= 11 - idx11
      ans ^= key11(idx11)
      ans ^= key13(idx13)
      ans += 13 - idx13
      
      idx11 = if (idx11 == 10) 0 else idx11 + 1 
      idx13 = if (idx13 == 12) 0 else idx13 + 1 

      ans & 0xFF
  }
}
