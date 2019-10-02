package org.rwtodd.bascat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;

/**
 * Utilities for reading MBF floating-point formats.
 *
 * @author Richard Todd
 */
abstract class MBFReader {

    public static double getMBF32(final ByteBuffer bb) {
        final var fourBytes = bb.getInt();
        if((fourBytes & 0xff00_0000) == 0) {
            return 0.0;
        } 
        final var sign = new BigDecimal((0x0080_0000 & fourBytes) == 0 ? 1 : -1, MathContext.DECIMAL128);
        final int exp = ((fourBytes >> 24) & 0xff) - 129;
        final var scaled = new BigDecimal(BigInteger.ONE.shiftLeft(Math.abs(exp)), MathContext.DECIMAL128);
        final var expt = (exp < 0) ? BigDecimal.ONE.divide(scaled, MathContext.DECIMAL128) : scaled;
        final var scand = new BigDecimal((fourBytes & 0x007f_ffff)|0x0080_0000, MathContext.DECIMAL128);
        return scand.divide(new BigDecimal(0x80_0000L, MathContext.DECIMAL128))
                .multiply(sign, MathContext.DECIMAL128)
                .multiply(expt, MathContext.DECIMAL128)
                .doubleValue();
    }

    public static double getMBF64(final ByteBuffer bb) {
        final var eightBytes = bb.getLong();
        if ((eightBytes & 0xff000000_00000000L) == 0) {
            return 0.0;
        }

        final var sign = new BigDecimal((0x00800000_00000000L & eightBytes) == 0 ? 1 : -1, MathContext.DECIMAL128);
        final int exp = (int)((eightBytes >> 56) & 0xff) - 129;
        final var scaled = new BigDecimal(BigInteger.ONE.shiftLeft(Math.abs(exp)), MathContext.DECIMAL128);
        final var expt = (exp < 0) ? BigDecimal.ONE.divide(scaled, MathContext.DECIMAL128) : scaled;
        var scand = new BigDecimal((eightBytes & 0x007fffff_ffffffffL)|0x00800000_00000000L, MathContext.DECIMAL128);
        return scand.divide(new BigDecimal(0x80_0000_0000_0000L, MathContext.DECIMAL128))
                .multiply(sign, MathContext.DECIMAL128)
                .multiply(expt, MathContext.DECIMAL128)
                .doubleValue();
    }

}
