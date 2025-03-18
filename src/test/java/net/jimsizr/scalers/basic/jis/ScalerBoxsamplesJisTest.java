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
package net.jimsizr.scalers.basic.jis;

import java.util.Random;

import junit.framework.TestCase;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.utils.Argb32;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisColorUtils;
import net.jimsizr.utils.JisUtils;

/**
 * Only tests the core interpolation method.
 */
public class ScalerBoxsamplesJisTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int NBR_OF_CALLS = 10 * 1000;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ScalerBoxsamplesJisTest() {
    }
    
    /*
     * 
     */
    
    public void test_computeBoxsampledColor32_uniform() {
        final int[] spanArr = new int[] {1,2,3,4,5};
        for (int width : spanArr) {
            for (int height : spanArr) {
                for (boolean opaque : new boolean[] {false,true}) {
                    test_computeBoxsampledColor32_uniform_xxx(
                        width,
                        height,
                        opaque);
                }
            }
        }
    }
    
    public void test_computeBoxsampledColor32_uniform_xxx(
        int width,
        int height,
        boolean opaque) {
        
        final BufferedImageHelper srcPixels =
            newSrcPixels(width, height);
        
        /*
         * Specified surface is clamped to srcRect,
         * so OK if it leaks out.
         */
        final double leakRatio = 0.1;
        final double leakFactor = (1.0 + leakRatio);
        
        final Random random = BihTestUtils.newRandom();
        final int nbrOfCalls = NBR_OF_CALLS;
        final double lowProba = Math.min(0.1, 10.0 / nbrOfCalls);
        for (int i = 0; i < nbrOfCalls; i++) {
            
            // Random color at each round.
            // Can use any alpha if not opaque,
            // since the only color change is due to conversion to premul,
            // not to (absent) interpolation.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            final int expectedNp = randomArgb32(random, minAlpha8);
            
            final int expectedP = JisColorUtils.toPremulAxyz32(expectedNp);
            for (int sj = 0; sj < height; sj++) {
                for (int si = 0; si < width; si++) {
                    srcPixels.setArgb32At(si, sj, expectedP, true);
                }
            }
            
            final double centerXFp;
            final double centerYFp;
            if (random.nextDouble() < lowProba) {
                // Exact center of rectangle.
                centerXFp = (width - 1) / 2.0;
                centerYFp = (height - 1) / 2.0;
            } else {
                centerXFp =
                    (width * leakFactor) * (random.nextDouble() - leakRatio/2);
                centerYFp =
                    (height * leakFactor) * (random.nextDouble() - leakRatio/2);
            }
            
            final double dxPixelSpanFp;
            if (random.nextDouble() < lowProba) {
                // integer span, possibly zero (tiny surface case)
                dxPixelSpanFp = random.nextInt(width + 1);
            } else {
                dxPixelSpanFp = (width * leakFactor) * random.nextDouble();
            }
            
            final double dyPixelSpanFp;
            if (random.nextDouble() < lowProba) {
                // integer span, possibly zero (tiny surface case)
                dyPixelSpanFp = random.nextInt(height + 1);
            } else {
                dyPixelSpanFp = (height * leakFactor) * random.nextDouble();
            }
            
            if (DEBUG) {
                System.out.println();
                System.out.println("width = " + width);
                System.out.println("height = " + height);
                System.out.println("opaque = " + opaque);
                System.out.println("expectedNp = " + Argb32.toString(expectedNp));
                System.out.println("centerXFp = " + centerXFp);
                System.out.println("centerYFp = " + centerYFp);
                System.out.println("dxPixelSpanFp = " + dxPixelSpanFp);
                System.out.println("dyPixelSpanFp = " + dyPixelSpanFp);
            }
            
            final PixelAccumulator pixelAccu = new PixelAccumulator();
            final int actualP = call_computeBoxsampledColor32_general(
                srcPixels,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                pixelAccu);
            
            final int cptDeltaTol = 0;
            checkEqualPremulArgb32(
                expectedP,
                actualP,
                cptDeltaTol);
        }
    }
    
    /*
     * 
     */
    
    public void test_computeBoxsampledColor32_exactPixel() {
        final int[] spanArr = new int[] {4,5};
        for (int width : spanArr) {
            for (int height : spanArr) {
                for (boolean opaque : new boolean[] {false,true}) {
                    test_computeBoxsampledColor32_exactPixel_xxx(
                        width,
                        height,
                        opaque);
                }
            }
        }
    }
    
    public void test_computeBoxsampledColor32_exactPixel_xxx(
        int width,
        int height,
        boolean opaque) {
        
        final BufferedImageHelper srcPixels =
            newSrcPixels(width, height);
        
        final Random random = BihTestUtils.newRandom();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            if (DEBUG) {
                System.out.println();
                System.out.println("width = " + width);
                System.out.println("height = " + height);
                System.out.println("opaque = " + opaque);
            }
            
            // Random color at each round.
            // Can use any alpha if not opaque,
            // since the only color change is due to conversion to premul,
            // not to (absent) interpolation.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            for (int sj = 0; sj < height; sj++) {
                for (int si = 0; si < width; si++) {
                    final int nonPremulArgb32 =
                        randomArgb32(random, minAlpha8);
                    srcPixels.setArgb32At(si, sj, nonPremulArgb32, false);
                }
            }

            final int x = random.nextInt(width);
            final int y = random.nextInt(height);
            final double centerXFp = x;
            final double centerYFp = y;
            final double dxPixelSpanFp = 1.0;
            final double dyPixelSpanFp = 1.0;
            
            final int expectedP = srcPixels.getPremulArgb32At(x, y);
            
            final PixelAccumulator pixelAccu = new PixelAccumulator();
            final int actualP = call_computeBoxsampledColor32_general(
                srcPixels,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                pixelAccu);
            
            final int cptDeltaTol = 0;
            checkEqualPremulArgb32(
                expectedP,
                actualP,
                cptDeltaTol);
        }
    }
    
    /*
     * 
     */
    
    public void test_computeBoxsampledColor32_2x2_oneOverFour() {
        for (boolean opaque : new boolean[] {false,true}) {
            test_computeBoxsampledColor32_2x2_oneOverFour_xxx(
                opaque);
        }
    }
    
    public void test_computeBoxsampledColor32_2x2_oneOverFour_xxx(
        boolean opaque) {
        
        final Random random = BihTestUtils.newRandom();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            final int alpha8 = uniform(random, minAlpha8, 0xFF);
            // Need tolerance depending on alpha,
            // since we check against theoretical result
            // computed in non-premul.
            final int cptDeltaTol = (alpha8 <= 0xFD ? 1 : 0);
            
            if (DEBUG) {
                System.out.println();
                System.out.println("opaque = " + opaque);
                System.out.println("alpha8 = " + alpha8);
                System.out.println("cptDeltaTol = " + cptDeltaTol);
            }
            
            final int width = 2;
            final int height = 2;
            
            final BufferedImageHelper srcPixels =
                newSrcPixels(width, height);
            
            /*
             * For each component: sum/4.
             * We use multiples of 4:
             * k * 4 = (k-2) + (k-1) + (k+1) + (k+2)
             * - with k = 11 :
             *   0x0B * 4 = 0x09 + 0x0A + 0x0C + 0x0D    
             * - with k = 12 :
             *   0x0C * 4 = 0x0A + 0x0B + 0x0D + 0x0E    
             * - with k = 13 :
             *   0x0D * 4 = 0x0B + 0x0C + 0x0E + 0x0F    
             * ===> That, but with two-digits components to avoid
             *      large relative error when interpolating in premul.
             */
            
            final int aNp = withAlpha8(0xFF99AABB, alpha8);
            final int bNp = withAlpha8(0xFFAABBCC, alpha8);
            final int cNp = withAlpha8(0xFFCCDDEE, alpha8);
            final int dNp = withAlpha8(0xFFDDEEFF, alpha8);
            
            srcPixels.setNonPremulArgb32At(0, 0, aNp);
            srcPixels.setNonPremulArgb32At(1, 0, bNp);
            srcPixels.setNonPremulArgb32At(0, 1, cNp);
            srcPixels.setNonPremulArgb32At(1, 1, dNp);
            
            final double centerXFp = 0.5;
            final double centerYFp = 0.5;
            final double dxPixelSpanFp = 1.0;
            final double dyPixelSpanFp = 1.0;
            
            final int expectedNp = withAlpha8(0xFFBBCCDD, alpha8);
            final int expectedP = JisColorUtils.toPremulAxyz32(expectedNp);
            
            final PixelAccumulator pixelAccu = new PixelAccumulator();
            final int actualP = call_computeBoxsampledColor32_general(
                srcPixels,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                pixelAccu);
            
            checkEqualPremulArgb32(
                expectedP,
                actualP,
                cptDeltaTol);
        }
    }
    
    /**
     * General case: with one full pixel and others partially covered.
     */
    public void test_computeBoxsampledColor32_2x2_generalCase() {
        for (boolean opaque : new boolean[] {false,true}) {
            test_computeBoxsampledColor32_2x2_generalCase_xxx(
                opaque);
        }
    }
    
    public void test_computeBoxsampledColor32_2x2_generalCase_xxx(
        boolean opaque) {
        
        final int srcWidth = 2;
        final int srcHeight = 2;
        
        final Random random = BihTestUtils.newRandom();
        final int nbrOfCalls = NBR_OF_CALLS;
        for (int i = 0; i < nbrOfCalls; i++) {
            
            // If not opaque, can use any alpha,
            // since we do an equivalent computation in the test.
            final int minAlpha8 = (opaque ? 0xFF : 0x00);
            
            final int aNp = randomArgb32(random, minAlpha8);
            final int bNp = randomArgb32(random, minAlpha8);
            final int cNp = randomArgb32(random, minAlpha8);
            final int dNp = randomArgb32(random, minAlpha8);
            
            /*
             * Pixel weights are 1, 1/p, 1/q, 1/(p*q).
             * We have (1 + 1/p + 1/q + 1/(p*q) = K)
             * i.e. (p*q + q + p + 1 = K * (p*q)).
             * With p=2, q=3: 6 + 3 + 2 + 1 = 12 = K * 6.
             * ===> K=2 (no need to, but remarkable).
             * 
             * Here we test that we give the proper weights
             * to covered pixels, so it's fine to just mimic
             * what the rest of the interpolation code does.
             */
            
            final double rp = 1.0 / 2;
            final double rq = 1.0 / 3;
            final double rpq = rp * rq;
            final double K = 1.0 + rp + rq + rpq;
            
            // Center at the center of either of the 4 pixels,
            // to test all 9 possible types of partial pixel covering code.
            final int centerIndex = random.nextInt(4);
            
            if (DEBUG) {
                System.out.println();
                System.out.println("opaque = " + opaque);
                System.out.println("aNp = " + Argb32.toString(aNp));
                System.out.println("bNp = " + Argb32.toString(bNp));
                System.out.println("cNp = " + Argb32.toString(cNp));
                System.out.println("dNp = " + Argb32.toString(dNp));
                System.out.println("centerIndex = " + centerIndex);
            }
            
            /*
             * Center and colors positions such as:
             * a ratio is 1 (where center is)
             * b ratio is rp
             * c ratio is rq
             * d ratio is rpq
             */
            final BufferedImageHelper srcPixels =
                newSrcPixels(srcWidth, srcHeight);
            final double centerXFp;
            final double centerYFp;
            if (centerIndex == 0) {
                centerXFp = 0.0;
                centerYFp = 0.0;
                srcPixels.setNonPremulArgb32At(0, 0, aNp);
                srcPixels.setNonPremulArgb32At(1, 0, bNp);
                srcPixels.setNonPremulArgb32At(0, 1, cNp);
                srcPixels.setNonPremulArgb32At(1, 1, dNp);
            } else if (centerIndex == 1) {
                centerXFp = 1.0;
                centerYFp = 0.0;
                srcPixels.setNonPremulArgb32At(0, 0, bNp);
                srcPixels.setNonPremulArgb32At(1, 0, aNp);
                srcPixels.setNonPremulArgb32At(0, 1, dNp);
                srcPixels.setNonPremulArgb32At(1, 1, cNp);
            } else if (centerIndex == 2) {
                centerXFp = 0.0;
                centerYFp = 1.0;
                srcPixels.setNonPremulArgb32At(0, 0, cNp);
                srcPixels.setNonPremulArgb32At(1, 0, dNp);
                srcPixels.setNonPremulArgb32At(0, 1, aNp);
                srcPixels.setNonPremulArgb32At(1, 1, bNp);
            } else if (centerIndex == 3) {
                centerXFp = 1.0;
                centerYFp = 1.0;
                srcPixels.setNonPremulArgb32At(0, 0, dNp);
                srcPixels.setNonPremulArgb32At(1, 0, cNp);
                srcPixels.setNonPremulArgb32At(0, 1, bNp);
                srcPixels.setNonPremulArgb32At(1, 1, aNp);
            } else {
                throw new AssertionError();
            }
            
            final double dxPixelSpanFp = 1.0 + 2 * rp;
            final double dyPixelSpanFp = 1.0 + 2 * rq;
            
            // Interpolation always done in premul by the algo.
            final int aP = JisColorUtils.toPremulAxyz32(aNp);
            final int bP = JisColorUtils.toPremulAxyz32(bNp);
            final int cP = JisColorUtils.toPremulAxyz32(cNp);
            final int dP = JisColorUtils.toPremulAxyz32(dNp);
            
            final double eAlpha8d =
                (Argb32.getAlpha8(aP)
                    + rp * Argb32.getAlpha8(bP)
                    + rq * Argb32.getAlpha8(cP)
                    + rpq * Argb32.getAlpha8(dP)) / K;
            final double eRed8d =
                (Argb32.getRed8(aP)
                    + rp * Argb32.getRed8(bP)
                    + rq * Argb32.getRed8(cP)
                    + rpq * Argb32.getRed8(dP)) / K;
            final double eGreen8d =
                (Argb32.getGreen8(aP)
                    + rp * Argb32.getGreen8(bP)
                    + rq * Argb32.getGreen8(cP)
                    + rpq * Argb32.getGreen8(dP)) / K;
            final double eBlue8d =
                (Argb32.getBlue8(aP)
                    + rp * Argb32.getBlue8(bP)
                    + rq * Argb32.getBlue8(cP)
                    + rpq * Argb32.getBlue8(dP)) / K;
            
            final int eAlpha8 = (int) (eAlpha8d + 0.5);
            final int eRed8 = (int) (eRed8d + 0.5);
            final int eGreen8 = (int) (eGreen8d + 0.5);
            final int eBlue8 = (int) (eBlue8d + 0.5);
            
            final int expectedP =
                JisColorUtils.toValidPremulAxyz32(
                    eAlpha8,
                    eRed8,
                    eGreen8,
                    eBlue8);
            
            final PixelAccumulator pixelAccu = new PixelAccumulator();
            final int actualP = call_computeBoxsampledColor32_general(
                srcPixels,
                centerXFp,
                centerYFp,
                dxPixelSpanFp,
                dyPixelSpanFp,
                //
                pixelAccu);
            
            if (DEBUG) {
                System.out.println("expectedP = " + Argb32.toString(expectedP));
                System.out.println("actualP =   " + Argb32.toString(actualP));
            }
            
            JisTestUtils.checkIsValidPremulArgb32(actualP);
            
            // One because might have ties due to rounding errors
            // (like 124.5 giving 125, vs 124.49999999999997 giving 124).
            final int cptDeltaTol = 1;
            checkEqualPremulArgb32(
                expectedP,
                actualP,
                cptDeltaTol);
        }
    }
    
    /*
     * 
     */
    
    public void test_computeBoxsampledColor32_general_tinySurface() {
        
        final int srcWidth = 10;
        final int srcHeight = 10;
        final BufferedImageHelper srcPixels =
            newSrcPixels(srcWidth, srcHeight);
        
        final Random random = BihTestUtils.newRandom();
        BihTestUtils.randomizeHelper(random, srcPixels, false);
        
        // Out of clip.
        final int centerX = -1;
        final int centerY = -1;
        
        final int expectedNp = randomArgb32(random, 0xFF);
        final int expectedP = JisColorUtils.toPremulAxyz32(expectedNp);
        // Pixel closest to center.
        srcPixels.setPremulArgb32At(0, 0, expectedP);
        
        double centerXFp = centerX;
        double centerYFp = centerY;
        
        double dxPixelSpanFp = 1.0 / Integer.MAX_VALUE;
        double dyPixelSpanFp = 1.0 / Integer.MAX_VALUE;
        
        final PixelAccumulator pixelAccu = new PixelAccumulator();
        final int actualP = call_computeBoxsampledColor32_general(
            srcPixels,
            centerXFp,
            centerYFp,
            dxPixelSpanFp,
            dyPixelSpanFp,
            //
            pixelAccu);
        
        final int cptDeltaTol = 0;
        checkEqualPremulArgb32(expectedP, actualP, cptDeltaTol);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Method to test ScalerBoxsampledJis.computeBoxsampledColor32_general()
     * more easily, by doing preliminary Y parameters computations
     * exactly as done in ScalerBoxsampledJis.
     * 
     * Package-private for use in bilinear test.
     */
    private static int call_computeBoxsampledColor32_general(
        BufferedImageHelper srcPixels,
        double centerXFp,
        double centerYFp,
        final double dxPixelSpanFp,
        final double dyPixelSpanFp,
        //
        PixelAccumulator pixelAccu) {
        
        final double H = 0.5;
        
        final int sh = srcPixels.getHeight();
        
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double yaFp = - H;
        final double ybFp = (sh - 1) + H;
        
        // Clamping.
        final double clpYMinFp = JisUtils.toRange(yaFp, ybFp, centerYFp - dyPixelSpanFp * H);
        final double clpYMaxFp = JisUtils.toRange(yaFp, ybFp, centerYFp + dyPixelSpanFp * H);
        //
        final double clpYSpanFp = clpYMaxFp - clpYMinFp;
        
        final double clpDyMinFp = (clpYMinFp - yaFp);
        final double clpDyMaxFp = (clpYMaxFp - yaFp);
        
        final int clpDyMinFloor = (int) clpDyMinFp;
        final int clpDyMaxFloor = (int) clpDyMaxFp;
        
        // When no pixel is fully covered in Y,
        // fullDyMin is the coordinate of the pixel after the one covered,
        // so as to always have loYRatio used for pixel at fullDyMin-1.
        final double loYRatio;
        final int fullDyMin;
        final int fullDyMax;
        final double hiYRatio;
        if ((clpDyMinFloor == clpDyMaxFloor)
            || (clpDyMinFloor + 1.0 == clpDyMaxFp)) {
            /*
             * Area in same Y row.
             * Will only use (loYRatio,fullDyMin-1).
             */
            loYRatio = (clpDyMaxFp - clpDyMinFp);
            fullDyMin = clpDyMinFloor + 1;
            fullDyMax = Integer.MIN_VALUE;
            hiYRatio = 0.0;
        } else {
            loYRatio = (clpDyMinFloor + 1 - clpDyMinFp);
            fullDyMin = clpDyMinFloor + 1;
            fullDyMax = clpDyMaxFloor - 1;
            hiYRatio = (clpDyMaxFp - clpDyMaxFloor);
        }
        
        return ScalerBoxsampledJis.computeBoxsampledColor32_general(
            srcPixels,
            centerXFp,
            centerYFp,
            dxPixelSpanFp,
            //
            clpYSpanFp,
            loYRatio,
            fullDyMin,
            fullDyMax,
            hiYRatio,
            //
            pixelAccu);
    }
    
    /*
     * 
     */
    
    private static BufferedImageHelper newSrcPixels(int width, int height) {
        return new BufferedImageHelper(
            BihTestUtils.newImageArgb(width, height));
    }
    
    /*
     * 
     */
    
    private static int withAlpha8(int argb32, int alpha8) {
        return (alpha8 << 24) | (argb32 & 0x00FFFFFF);
    }
    
    private static int randomArgb32(Random random, int minAlpha8) {
        int argb32 = BihTestUtils.randomArgb32(random, false);
        if (minAlpha8 != 0xFF) {
            final int alpha8 = uniform(random, minAlpha8, 0xFF);
            argb32 = withAlpha8(argb32, alpha8);
        }
        return argb32;
    }
    
    private static int uniform(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * Always interpolating in premul,
     * so if color type is not premul,
     * we convert expected and actual to premul before comparison,
     * to simulate the same conversion error.
     */
    private static void checkEqualPremulArgb32(
        int expectedP,
        int actualP,
        int cptDeltaTol) {
        
        final int maxCptDelta =
            BihTestUtils.getMaxCptDelta(
                expectedP,
                actualP);
        if (maxCptDelta > cptDeltaTol) {
            System.out.println("expectedP = " + Argb32.toString(expectedP));
            System.out.println("actualP = " + Argb32.toString(actualP));
            throw new AssertionError(maxCptDelta + " > " + cptDeltaTol);
        }
    }
}
