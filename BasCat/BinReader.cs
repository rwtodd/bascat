using System;
using System.Collections.Generic;
using System.Numerics;
using System.Text;

namespace BasCat
{
    class BinReader
    {
        private int idx;
        private byte[] buf;
        private readonly int last2;
        private byte[] mbf;  // scratch space for use in floating-pt conversion

        public BinReader(byte[] buffer)
        {
            buf = buffer;
            idx = 0;
            last2 = buf.Length - 1;
            mbf = new byte[8];
        }

        public bool EOF
        {
            get => idx >= buf.Length;
        }

        public byte ReadByte()
        {
            return (idx < buf.Length) ? buf[idx++] : (byte)0;
        }

        public bool Peek(byte val) => !EOF && buf[idx] == val;
        public bool Peek2(byte v1, byte v2) => (idx < last2) && (buf[idx] == v1) && (buf[idx + 1] == v2);

        public void Skip(int offs)
        {
            idx += offs;
        }

        public UInt16 ReadU16()
        {
            var ans = (idx < last2) ? (buf[idx] | (buf[idx + 1] << 8)) : 0;
            idx += 2;
            return (UInt16)ans;
        }

        public Int16 ReadS16() => (Int16)ReadU16();

        public Single ReadMBF32()
        {
            mbf[0] = ReadByte();
            mbf[1] = ReadByte();
            mbf[2] = ReadByte();
            mbf[3] = ReadByte();
            if (mbf[3] == 0) return 0.0f;

            byte sign = (byte)(mbf[2] & 0x80);
            byte exp = (byte)(mbf[3] - 2);
            mbf[3] = (byte)(sign | ((exp >> 1) & 0xff));
            mbf[2] = (byte)((exp << 7) | (mbf[2] & 0x7F));

            if (!System.BitConverter.IsLittleEndian) System.Array.Reverse(mbf, 0, 4);
            return System.BitConverter.ToSingle(mbf, 0);
        }

        public Double ReadMBF64()
        {
            for (int idx = 0; idx < 8; ++idx)
            {
                mbf[idx] = ReadByte();
            }
            if (mbf[7] == 0) return 0.0;

            Byte sign = (byte)(mbf[6] & 0x80);  // save off the sign
            mbf[6] &= 0x7f;  // erase the sign bit
            Int16 exp = (Int16)(mbf[7] - 129 + 1023);

            // shift over the significand by 3 bits (55 in ieee, 58 in mbf)
            for (int idx = 0; idx < 7; ++idx)
            {
                mbf[idx] = (byte)((mbf[idx] >> 3) | (mbf[idx + 1] << 5));
            }

            // now fix up the top bytes...
            // exp 16 bits == FEDCB(A987654)(3210)
            mbf[7] = (byte)(sign | ((exp >> 4) & 0x7f));
            mbf[6] = (byte)((mbf[6] & 0x0F) | ((exp & 0x0f) << 4));

            if (!System.BitConverter.IsLittleEndian) System.Array.Reverse(mbf);
            return System.BitConverter.ToDouble(mbf, 0);
        }
    }
}
