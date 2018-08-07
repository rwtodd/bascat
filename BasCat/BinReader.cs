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

        public BinReader(byte[] buffer)
        {
            buf = buffer;
            idx = 0;
            last2 = buf.Length - 1;
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

        public Double ReadF32()
        {
            if (idx > buf.Length - 4) { return 0.0; }
            var bs0 = (Int32)buf[idx++];
            var bs1 = (Int32)buf[idx++];
            var bs2 = (Int32)buf[idx++];
            var bs3 = (Int32)buf[idx++];
            if (bs3 == 0) { return 0.0; }

            var sign = new BigInteger(((0x80 & bs2) == 0) ? 1 : -1);
            var exp = bs3 - 129;
            var TWO = new BigInteger(2);
            var expt = BigInteger.Pow(TWO, Math.Abs(exp));
            // var scand = new BigInteger(bs0 | (bs1 << 8) | ((bs2 & 0x7f) << 16));
            var scand = new BigInteger(bs0 | (bs1 << 8) | ((bs2 | 0x80) << 16));
            var numer = sign * scand;
            var denom = new BigInteger(0x800000L);
            if (exp >= 0) { numer = numer * expt; } else { denom = denom * expt; }
            return numer.DivideAndReturnDouble(denom);
         }

    public Double ReadF64()
    {
        idx += 8;
        return 0.0;
    }
}

internal static class BigIntExtensions
{
    public static double DivideAndReturnDouble(this BigInteger x, BigInteger y)
    {
        // The Double value type represents a double-precision 64-bit number with
        // values ranging from -1.79769313486232e308 to +1.79769313486232e308
        // values that do not fit into this range are returned as +/-Infinity
        if (SafeCastToDouble(x) && SafeCastToDouble(y))
        {
            return (Double)x / (Double)y;
        }

        // kick it old-school and figure out the sign of the result
        bool isNegativeResult = ((x.Sign < 0 && y.Sign > 0) || (x.Sign > 0 && y.Sign < 0));

        // scale the numerator to preseve the fraction part through the integer division
        BigInteger denormalized = (x * s_bnDoublePrecision) / y;
        if (denormalized.IsZero)
        {
            return isNegativeResult ? BitConverter.Int64BitsToDouble(unchecked((long)0x8000000000000000)) : 0d; // underflow to -+0
        }

        Double result = 0;
        bool isDouble = false;
        int scale = DoubleMaxScale;

        while (scale > 0)
        {
            if (!isDouble)
            {
                if (SafeCastToDouble(denormalized))
                {
                    result = (Double)denormalized;
                    isDouble = true;
                }
                else
                {
                    denormalized = denormalized / 10;
                }
            }
            result = result / 10;
            scale--;
        }

        if (!isDouble)
        {
            return isNegativeResult ? Double.NegativeInfinity : Double.PositiveInfinity;
        }
        else
        {
            return result;
        }

    }

    private const int DoubleMaxScale = 308;
    private static readonly BigInteger s_bnDoublePrecision = BigInteger.Pow(10, DoubleMaxScale);
    private static readonly BigInteger s_bnDoubleMaxValue = (BigInteger)Double.MaxValue;
    private static readonly BigInteger s_bnDoubleMinValue = (BigInteger)Double.MinValue;

    private static bool SafeCastToDouble(BigInteger value)
    {
        return s_bnDoubleMinValue <= value && value <= s_bnDoubleMaxValue;
    }

}
}
