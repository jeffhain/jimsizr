/*
 * Copyright 2025 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jimsizr.utils;

/**
 * Convention for color values naming:
 * - {_,_,_,_} : abcd
 * - {alpha,_,_,_} : axyz
 */
public class JisColorUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Our guard against used entering providing
     * invalid alpha-premultiplied values.
     */
    private static final boolean MUST_DO_PREMUL_CHECKS = true;
    
    /**
     * Could be true if only writing pixels on fully opaque destinations.
     */
    static final boolean CAN_ASSUME_THAT_DST_ALPHA_8_IS_255 = false;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisColorUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param a8 Must be in [0,255].
     * @param b8 Must be in [0,255].
     * @param c8 Must be in [0,255].
     * @param d8 Must be in [0,255].
     * @return The corresponding ABCD32 color.
     */
    public static int toAbcd32_noCheck(int a8, int b8, int c8, int d8) {
        int i = a8;
        i <<= 8;
        i |= b8;
        i <<= 8;
        i |= c8;
        i <<= 8;
        i |= d8;
        return i;
    }
    
    public static void checkValidPremulAxyz(int alpha8, int x8, int y8, int z8) {
        if ((x8 > alpha8)
            || (y8 > alpha8)
            || (z8 > alpha8)) {
            final int axyz = toAbcd32_noCheck(alpha8, x8, y8, z8);
            throw new IllegalArgumentException(
                Argb32.toString(axyz)
                + " -> premul Abcd = "
                + alpha8 + ", " + x8 + ", " + y8 + ", " + z8);
        }
    }
    
    /*
     * 
     */
    
    /**
     * @param cpt8 Must be in [0,255].
     * @return The corresponding floating-point value in [0,1].
     */
    public static double toFpFromInt8_noCheck(int cpt8) {
        return cpt8 * (1.0/255);
    }
    
    /*
     * Conversions between ARGB, and ABGR or RGBA.
     * These two last formats are useful to deal with
     * OpenGL libraries in big or little endian.
     * 
     * Also additional conversions to cover all BihPixelFormat needs.
     */
    
    /*
     * ARGB <-> ABGR
     */
    
    public static int toArgb32FromAbgr32(int abgr32) {
        return switch_2_4(abgr32);
    }
    
    public static int toAbgr32FromArgb32(int argb32) {
        return switch_2_4(argb32);
    }
    
    /*
     * ARGB <-> RGBA
     */
    
    public static int toArgb32FromRgba32(int rgba32) {
        return rotateRightOneByte(rgba32);
    }
    
    public static int toRgba32FromArgb32(int argb32) {
        return rotateLeftOneByte(argb32);
    }
    
    /*
     * ARGB <-> BGRA
     */
    
    public static int toArgb32FromBgra32(int bgra32) {
        return Integer.reverseBytes(bgra32);
    }
    
    public static int toBgra32FromArgb32(int argb32) {
        return Integer.reverseBytes(argb32);
    }
    
    /*
     * 
     */
    
    /**
     * Switches second and fourth bytes.
     */
    public static int switch_2_4(int xuxv) {
        // X_X_ | _V__ | ___U
        return (xuxv & 0xFF00FF00)
            | ((xuxv & 0x000000FF) << 16)
            | ((xuxv & 0x00FF0000) >>> 16);
    }
    
    /**
     * Switches first and third bytes.
     */
    public static int switch_1_3(int uxvx) {
        // _X_X_ | V___ | __U_
        return (uxvx & 0x00FF00FF)
            | ((uxvx & 0x0000FF00) << 16)
            | ((uxvx & 0xFF000000) >>> 16);
    }
    
    /**
     * @return xyyy
     */
    public static int rotateRightOneByte(int yyyx) {
        // X___ | _YYY
        return (yyyx << 24) | (yyyx >>> 8);
    }
    
    /**
     * @return yyyx
     */
    public static int rotateLeftOneByte(int xyyy) {
        // ___X | YYY_
        return (xyyy >>> 24) | (xyyy << 8);
    }
    
    /*
     * 
     */
    
    /**
     * @param cpt8 Must be in [0,255].
     * @param alphaFp Must be in [0,1].
     * @return The corresponding premultiplied component, in [0,255].
     */
    public static int toPremul8_noCheck(int cpt8, double alphaFp) {
        return toInt8FromFp255_noCheck(alphaFp * cpt8);
    }
    
    /**
     * @param premulCpt8 Must be in [0,255].
     * @param oneOverAlphaFp Must be in [0,255/premulCpt8].
     * @return The corresponding non-premultiplied component.
     */
    public static int toNonPremul8_noCheck(int premulCpt8, double oneOverAlphaFp) {
        return toInt8FromFp255_noCheck(premulCpt8 * oneOverAlphaFp);
    }
    
    /*
     * Non premul color to premul color.
     */

    /**
     * @param axyz32 An AXYZ32 color, not alpha-premultiplied.
     * @return The corresponding AXYZ32 color, alpha-premultiplied.
     */
    public static int toPremulAxyz32(int axyz32) {
        final int alpha8 = Argb32.getAlpha8(axyz32);
        if (alpha8 == 0xFF) {
            return axyz32;
        }
        
        int x8 = Argb32.getRed8(axyz32);
        int y8 = Argb32.getGreen8(axyz32);
        int z8 = Argb32.getBlue8(axyz32);
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        
        x8 = toPremul8_noCheck(x8, alphaFp);
        y8 = toPremul8_noCheck(y8, alphaFp);
        z8 = toPremul8_noCheck(z8, alphaFp);
        
        if (MUST_DO_PREMUL_CHECKS) {
            checkValidPremulAxyz(alpha8, x8, y8, z8);
        }
        
        final int premulAxyz32 = toAbcd32_noCheck(alpha8, x8, y8, z8);
        
        return premulAxyz32;
    }

    /**
     * @param xyza32 An XYZA32 color, not alpha-premultiplied.
     * @return The corresponding XYZA32 color, alpha-premultiplied.
     */
    public static int toPremulXyza32(int xyza32) {
        final int alpha8 = Argb32.getBlue8(xyza32);
        if (alpha8 == 0xFF) {
            return xyza32;
        }
        
        int x8 = Argb32.getAlpha8(xyza32);
        int y8 = Argb32.getRed8(xyza32);
        int z8 = Argb32.getGreen8(xyza32);
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        
        x8 = toPremul8_noCheck(x8, alphaFp);
        y8 = toPremul8_noCheck(y8, alphaFp);
        z8 = toPremul8_noCheck(z8, alphaFp);

        if (MUST_DO_PREMUL_CHECKS) {
            checkValidPremulAxyz(alpha8, x8, y8, z8);
        }

        return toAbcd32_noCheck(x8, y8, z8, alpha8);
    }
    
    /*
     * Premul color to non premul color.
     */

    /**
     * @param axyz32 An AXYZ32 color, alpha-premultiplied.
     * @return The corresponding AXYZ32 color, not alpha-premultiplied.
     */
    public static int toNonPremulAxyz32(int premulAxyz32) {
        final int alpha8 = Argb32.getAlpha8(premulAxyz32);
        if (alpha8 == 0xFF) {
            return premulAxyz32;
        }
        
        int x8 = Argb32.getRed8(premulAxyz32);
        int y8 = Argb32.getGreen8(premulAxyz32);
        int z8 = Argb32.getBlue8(premulAxyz32);
        
        if (MUST_DO_PREMUL_CHECKS) {
            checkValidPremulAxyz(alpha8, x8, y8, z8);
        }

        // Avoiding division by 0.
        if (alpha8 == 0) {
            // Must be (0,0,0,0) in premul.
            return 0;
        }
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        final double oneOverAlphaFp = 1.0/alphaFp;
        
        x8 = toNonPremul8_noCheck(x8, oneOverAlphaFp);
        y8 = toNonPremul8_noCheck(y8, oneOverAlphaFp);
        z8 = toNonPremul8_noCheck(z8, oneOverAlphaFp);
        
        return toAbcd32_noCheck(alpha8, x8, y8, z8);
    }
    
    /**
     * @param xyza32 An XYZA32 color, alpha-premultiplied.
     * @return The corresponding XYZA32 color, not alpha-premultiplied.
     */
    public static int toNonPremulXyza32(int premulXyza32) {
        final int alpha8 = Argb32.getBlue8(premulXyza32);
        if (alpha8 == 0xFF) {
            return premulXyza32;
        }
        
        int x8 = Argb32.getAlpha8(premulXyza32);
        int y8 = Argb32.getRed8(premulXyza32);
        int z8 = Argb32.getGreen8(premulXyza32);
        
        if (MUST_DO_PREMUL_CHECKS) {
            checkValidPremulAxyz(alpha8, x8, y8, z8);
        }

        // Avoiding division by 0.
        if (alpha8 == 0) {
            // Must be (0,0,0,0) in premul.
            return 0;
        }
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        final double oneOverAlphaFp = 1.0/alphaFp;
        
        x8 = toNonPremul8_noCheck(x8, oneOverAlphaFp);
        y8 = toNonPremul8_noCheck(y8, oneOverAlphaFp);
        z8 = toNonPremul8_noCheck(z8, oneOverAlphaFp);
        
        return toAbcd32_noCheck(x8, y8, z8, alpha8);
    }
    
    /*
     * 
     */
    
    /**
     * @param a8 Alpha component, possibly outside [0,255] range.
     * @param x8 First premul color component, possibly outside [0,255] range.
     * @param y8 Second color premul component, possibly outside [0,255] range.
     * @param z8 Third color premul component, possibly outside [0,255] range.
     * @return Corresponding valid premul value, i.e. with non-alpha components
     *         within [0,alpha8] range.
     */
    public static int toValidPremulAxyz32(int a8, int x8, int y8, int z8) {
        a8 = saturate(a8, 0xFF);
        x8 = saturate(x8, a8);
        y8 = saturate(y8, a8);
        z8 = saturate(z8, a8);
        return JisColorUtils.toAbcd32_noCheck(a8, x8, y8, z8);
    }
    
    /**
     * cf. SAT in src/java.desktop/share/native/libawt/java2d/loops/TransformHelper.c
     * 
     * @param val A value.
     * @param max A value >= 0.
     * @return Val taken back into in [0,max].
     */
    public static int saturate(int val, int max) {
        val &= ~(val >> 31);  /* negatives become 0 */
        val -= max;           /* only overflows are now positive */
        val &= (val >> 31);   /* positives become 0 */
        val += max;           /* range is now [0 -> max] */
        return val;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param valueFp255 Must be in [0,255].
     * @return The corresponding integer value in [0,255].
     */
    private static int toInt8FromFp255_noCheck(double valueFp255) {
        /*
         * Using "(int) (valueFp * Math.nextDown(256.0))" would give
         * the same floating point span for each integer value,
         * instead of 0.5/255 span for 0 and 255, and 1.0/255
         * for the other 254 values.
         * But no library seems to do that, so not to surprise users
         * we stick to the usual way.
         * 
         * NB: These kinds of optimizations and approximations cause
         * toNonPremul(toPremul(np)) conversions to often not be identity,
         * even for alphas as high as 0xFE.
         * Ex:
         * - Smallest value case:
         *   np = 0x01010101 gives p = 0x01000000,
         *   so cptP8=0, alpha8=1,
         *   cpt8NpBis=(int)(0*255.0/1+0.5)=0,
         *   which causes npBis = 0x01000000.
         * - Largest value case:
         *   np = 0xFE7F7F7F gives p = 0xFE7F7F7F (same),
         *   so cptP8=0x7F=127, alpha8=0xFE=254,
         *   cpt8NpBis=(int)(127*255.0/254+0.5)=128=0x80,
         *   which causes npBis = 0xFE808080.
         */
        return (int) (valueFp255 + 0.5);
    }
}
