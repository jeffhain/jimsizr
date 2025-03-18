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

import java.awt.image.BufferedImage;

public final class JisUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Because on some architectures, some casts can be slow,
     * especially for large values.
     * Might make things a bit slower for latest architectures,
     * but not as much as it makes them faster for older ones.
     */
    private static final boolean ANTI_SLOW_CASTS = true;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final int[] EMPTY_INT_ARR = new int[0];

    public static final double[] EMPTY_DOUBLE_ARR = new double[0];
    
    private static final int MAX_DOUBLE_EXPONENT = 1023;

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------

    private JisUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param ref A reference.
     * @return The specified reference (if it is non-null).
     * @throws NullPointerException if the specified reference is null.
     */
    public static <T> T requireNonNull(T ref) {
        if (ref == null) {
            throw new NullPointerException();
        }
        return ref;
    }
    
    /*
     * min/max ranges
     */
    
    /**
     * @return True if the specified value is in the specified range (inclusive), false otherwise.
     */
    public static boolean isInRange(int min, int max, int value) {
        return (min <= value) && (value <= max);
    }
    
    /*
     * Requires in range.
     */
    
    /**
     * @param min Min value.
     * @param max Max value.
     * @param value A value.
     * @param name Value's name.
     * @return The specified value.
     * @throws IllegalArgumentException if the specified value is out of [min,max].
     */
    public static int requireInRange(int min, int max, int value, String name) {
        if (!isInRange(min, max, value)) {
            throw new IllegalArgumentException(
                name + " [" + value + "] must be in [" + min + "," + max + "]");
        }
        return value;
    }
    
    /*
     * Requires > min.
     */
    
    /**
     * @param min Min value.
     * @param value A value.
     * @param name Value's name.
     * @return The specified value.
     * @throws IllegalArgumentException if the specified value is <= min
     *         or any argument is NaN.
     */
    public static double requireSup(double min, double value, String name) {
        if (!(value > min)) {
            throw new IllegalArgumentException(
                name + " [" + value + "] must be > " + min);
        }
        return value;
    }
    
    /*
     * Requires >= min.
     */
    
    /**
     * @param min Min value.
     * @param value A value.
     * @param name Value's name.
     * @return The specified value.
     * @throws IllegalArgumentException if the specified value is < min.
     */
    public static int requireSupOrEq(int min, int value, String name) {
        if (!(value >= min)) {
            throw new IllegalArgumentException(
                name + " [" + value + "] must be >= " + min);
        }
        return value;
    }
    
    /**
     * @param min Min value.
     * @param value A value.
     * @param name Value's name.
     * @return The specified value.
     * @throws IllegalArgumentException if the specified value is < min
     *         or any argument is NaN.
     */
    public static double requireSupOrEq(double min, double value, String name) {
        if (!(value >= min)) {
            throw new IllegalArgumentException(
                name + " [" + value + "] must be >= " + min);
        }
        return value;
    }
    
    /*
     * Requires <= max.
     */
    
    /**
     * @param max Max value.
     * @param value A value.
     * @param name Value's name.
     * @return The specified value.
     * @throws IllegalArgumentException if the specified value is > max.
     */
    public static long requireInfOrEq(long max, long value, String name) {
        if (!(value <= max)) {
            throw new IllegalArgumentException(
                name + " [" + value + "] must be <= " + max);
        }
        return value;
    }
    
    /*
     * 
     */

    /**
     * @param min A value.
     * @param max A value.
     * @param a A value.
     * @return min if a <= min, else max if a >= max, else a.
     */
    public static int toRange(int min, int max, int a) {
        if (a <= min) {
            return min;
        } else if (a >= max) {
            return max;
        } else {
            return a;
        }
    }
    
    /**
     * @param min A value.
     * @param max A value.
     * @param a A value.
     * @return min if a <= min, else max if a >= max, else a.
     */
    public static double toRange(double min, double max, double a) {
        if (a <= min) {
            return min;
        } else if (a >= max) {
            return max;
        } else {
            return a;
        }
    }
    
    /*
     * 
     */
    
    /**
     * @param a A long value.
     * @return The closest int value.
     */
    private static int toInt(long a) {
        int ret = (int) a;
        if (ret != a) {
            ret = (a < 0) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * @param a An int value.
     * @param b An int value.
     * @return The mathematical result of a*b.
     * @throws ArithmeticException if the mathematical result of a*b
     *         is not in [Integer.MIN_VALUE,Integer.MAX_VALUE] range.
     */
    public static int timesExact(int a, int b) {
        final long prod = a * (long) b;
        if (prod != (int) prod) {
            throw new ArithmeticException("overflow: " + a + "*" + b);
        }
        return (int) prod;
    }
    
    /*
     * close values (that are either not in Math, or bugged in some JDK versions)
     */
    
    /**
     * Might have different semantics than Math.round(double),
     * see bugs 6430675 and 8010430.
     * 
     * @param value A double value.
     * @return Value rounded to nearest long, choosing superior long in case two
     *         are equally close (i.e. rounding-up).
     */
    private static long round(double value) {
        /*
         * Not delegating to JDK, because we want delegation to provide
         * at least as good results, and some supported JDK versions
         * have bugged round() methods.
         */
        final long bits = Double.doubleToRawLongBits(value);
        final int biasedExp = (((int)(bits>>52))&0x7FF);
        // Shift to get rid of bits past comma except first one: will need to
        // 1-shift to the right to end up with correct magnitude.
        final int shift = (52 - 1 + MAX_DOUBLE_EXPONENT) - biasedExp;
        if ((shift & -64) == 0) {
            long bitsSignum = (((bits >> 63) << 1) + 1);
            // shift in [0,63], so unbiased exp in [-12,51].
            long extendedMantissa = (0x0010000000000000L | (bits & 0x000FFFFFFFFFFFFFL)) * bitsSignum;
            // If value is positive and first bit past comma is 0, rounding
            // to lower integer, else to upper one, which is what "+1" and
            // then ">>1" do.
            return ((extendedMantissa >> shift) + 1L) >> 1;
        } else {
            // +-Infinity, NaN, or a mathematical integer, or tiny.
            if (ANTI_SLOW_CASTS) {
                if (Math.abs(value) >= -(double)Long.MIN_VALUE) {
                    // +-Infinity or a mathematical integer (mostly) out of long range.
                    return (value < 0.0) ? Long.MIN_VALUE : Long.MAX_VALUE;
                }
                // NaN or a mathematical integer (mostly) in long range.
            }
            return (long)value;
        }
    }

    /*
     * close int values
     * 
     * Never delegating to JDK for these methods, for we should always
     * be faster and exact, and JDK doesn't exactly have such methods.
     */

    /**
     * Preferable to "(int) Math.rint(double)", because in case of ties
     * round() rounds to upper value, and rint() to even value,
     * which can cause irregularities (pixel choice depending
     * on whether coordinates are even or odd).
     * 
     * @param value A double value.
     * @return Value rounded to nearest int, choosing superior int in case two
     *         are equally close (i.e. rounding-up).
     */
    public static int roundToInt(double value) {
        return JisUtils.toInt(round(value));
    }
    
    /*
     * 
     */
    
    public static void checkNoScaling(
        BufferedImage srcImage,
        BufferedImage dstImage) {
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        final boolean gotXScaling = (dw != sw);
        final boolean gotYScaling = (dh != sh);
        if (gotXScaling || gotYScaling) {
            throw new IllegalArgumentException("got scaling");
        }
    }
}
