/*
 * Copyright (C) 2019 Richard Todd
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.rwtodd.bascat;

/**
 * A decrypter for protected BAS files. I found the algorithm in a python
 * program ("PC-BASIC"), ({@link  http://sourceforge.net/p/pcbasic/wiki/Home/})
 * ... but the algorithm was published in: The Cryptogram computer supplement
 * #19, American Cryptogram Association, Summer 1994
 *
 * Basically there is a 13-byte and an 11-byte key, which are BOTH applied in a
 * cycle across the bytes of the input. Also, a reversed 11-index is subtracted
 * from the byte, while a reversed 13-index is added to the byte. By 'reversed',
 * I mean that as the 11-index goes from 0 to 10, the reversed index goes from
 * 11 to 1.
 *
 * @author richard todd
 */
final class Unprotector {

    private final static int[] KEY13 = {0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83,
        0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A};
    private final static int[] KEY11 = {0x1E, 0x1D, 0xC4, 0x77, 0x26,
        0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C};

    public static byte[] unprotect(byte[] src) {
        int idx13 = 0;
        int idx11 = 0;
        int ans;

        src[0] = (byte) 0xff; // no more need to unprotect
        for (int idx = 1; idx < src.length; idx++) {
            ans = src[idx] & 0xff;
            ans -= 11 - idx11;
            ans ^= KEY11[idx11];
            ans ^= KEY13[idx13];
            ans += 13 - idx13;
            src[idx] = (byte) ans;

            idx11++;
            idx13++;
            if (idx11 == 11) {
                idx11 = 0;
            }
            if (idx13 == 13) {
                idx13 = 0;
            }
        }
        return src;
    }
}
