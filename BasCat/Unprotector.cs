namespace BasCat
{
    internal class Unprotector
    {
        static byte[] key11 = { 0x1E, 0x1D, 0xC4, 0x77, 0x26,
                            0x97, 0xE0, 0x74, 0x59, 0x88, 0x7C };
        static byte[] key13 = { 0xA9, 0x84, 0x8D, 0xCD, 0x75, 0x83,
                          0x43, 0x63, 0x24, 0x83, 0x19, 0xF7, 0x9A };

        public static byte[] decode(byte[] buffer)
        {
            int idx11 = 0;
            int idx13 = 0;
            
            for(int idx = 0; idx < buffer.Length; ++idx)
            {
                var ans = buffer[idx] - (11 - idx11);
                ans ^= key11[idx11];
                ans ^= key13[idx13];
                ans += (13 - idx13);
                buffer[idx] = (byte)ans;
            }
            return buffer;
        }
    }
}