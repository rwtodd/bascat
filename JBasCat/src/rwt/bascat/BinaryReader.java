/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rwt.bascat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntSupplier;

/**
 * Utilities for reading binary data.
 * @author richa
 */
public class BinaryReader {
    final IntSupplier src;
    final ByteBuffer buf;
    
    public BinaryReader(IntSupplier source) {
        src = source;
        buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public int readu8() { return src.getAsInt(); }
    
    public int read16() {
        buf.clear();
        buf.put((byte)src.getAsInt()).put((byte)src.getAsInt()).flip();
        return buf.getShort();
    }

    public int readu16() { return read16() & 0xFFFF; }

    public double readf32() {
      buf.clear();
      buf.put((byte)src.getAsInt()).
          put((byte)src.getAsInt()).
          put((byte)src.getAsInt()).
          put((byte)src.getAsInt()).
          flip();
      final byte[] bs = buf.array();

     // convert MBF to IEEE
     //  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee
     if (bs[3] != 0) {
                final int sign = bs[2] & 0x80;
                final int exp = (bs[3] - 2) & 0xFF;
                bs[3] = (byte)(sign | (exp >> 1));
                bs[2] = (byte)((exp << 7) | (bs[2] & 0x7F));
                return buf.getFloat();
     }
     return 0.0;
  }

  public double readf64() {
     byte[] bs = { 
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt(),
         (byte)src.getAsInt()
     };
     
     // convert MBF to IEEE
     //  http://stackoverflow.com/questions/2973913/convert-mbf-single-and-double-to-ieee

     if (bs[7] != 0) {
                byte[] iees = new byte[8];

                final int sign = bs[6] & 0x80;
                final int exp = (bs[3] - 128 - 1 + 1023) & 0xFFFF;

                iees[7] = (byte)(sign | (exp>>4));
                iees[6] = (byte)(exp << 4);

                for(int idx = 6; idx >= 1; --idx) {
                    bs[idx] = (byte) ( ( (bs[idx] << 1) | (bs[idx-1] >> 7) ) & 0xFF );
                    iees[idx] = (byte)(iees[idx] | (bs[idx] >> 4));
                    iees[idx-1] = (byte)(iees[idx-1] | (bs[idx] << 4));
                }
                bs[0] = (byte)(bs[0] << 1);
                iees[0] = (byte)(iees[0] | (bs[0] >> 4));

                return ByteBuffer.wrap(iees).order(ByteOrder.LITTLE_ENDIAN).getDouble();
     }
     return 0.0;
  } 
   
}
