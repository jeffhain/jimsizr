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

import java.awt.image.BufferedImage;

import net.jimsizr.scalers.implutils.AbstractParallelScaler;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

public class ScalerBicubicJis extends AbstractParallelScaler {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source area surface doesn't count (if ignoring memory read overhead).
     */
    private static final int SRC_AREA_THRESHOLD_FOR_SPLIT = Integer.MAX_VALUE;
    
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 512;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * For bicubic interpolation.
     */
    private static final double A = -0.5;
    
    /*
     * 
     */
    
    private final int dstAreaThresholdForSplit;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerBicubicJis() {
        this(DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT);
    }
    
    /**
     * @param dstAreaThresholdForSplit Must be >= 2.
     */
    public ScalerBicubicJis(int dstAreaThresholdForSplit) {
        this.dstAreaThresholdForSplit =
            JisUtils.requireSupOrEq(2, dstAreaThresholdForSplit, "dstAreaThresholdForSplit");
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName()
            + ",dstSplit=" + this.dstAreaThresholdForSplit
            + "]";
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getSrcAreaThresholdForSplit() {
        return SRC_AREA_THRESHOLD_FOR_SPLIT;
    }
    
    @Override
    protected int getDstAreaThresholdForSplit() {
        return this.dstAreaThresholdForSplit;
    }
    
    @Override
    protected void scaleImageChunk(
        BufferedImageHelper srcHelper,
        //
        int dstYStart,
        int dstYEnd,
        BufferedImageHelper dstHelper,
        //
        Object runData) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        // dst pixel width in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double dxPixelSpanFp = sw / (double) dw;
        // dst pixel height in src pixels,
        // when scaled up/down to match its corresponding src pixels.
        final double dyPixelSpanFp = sh / (double) dh;
        
        // +-0.5 needed due to integer coordinates
        // corresponding to pixels centers.
        final double H = 0.5;
        
        final int djStart = dstYStart;
        final int djEnd = djStart + (dstYEnd - dstYStart);
        
        final double[] tmpWxArr = new double[4];
        final PixelAccumulator pixelAccu = new PixelAccumulator();

        // Loop on srcY and inside on srcX (memory-friendly).
        for (int dj = djStart; dj <= djEnd; dj++) {
            // y in src of destination pixel's center. 
            final double srcYFp = (dj + H) * dyPixelSpanFp - H;
            
            final int syFloor = (int) Math.floor(srcYFp);
            final double syFracFp = srcYFp - syFloor;

            for (int di = 0; di < dw; di++) {
                // x in src of destination pixel's center. 
                final double srcXFp = (di + H) * dxPixelSpanFp - H;
                
                final int sxFloor = (int) Math.floor(srcXFp);
                final double sxFracFp = srcXFp - sxFloor;
                
                final int dstPremulArgb32 =
                    bicubicInterpolate(
                        srcHelper,
                        sxFloor,
                        syFloor,
                        sxFracFp,
                        syFracFp,
                        //
                        tmpWxArr,
                        pixelAccu);
                dstHelper.setPremulArgb32At(
                    di,
                    dj,
                    dstPremulArgb32);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int bicubicInterpolate(
        BufferedImageHelper srcHelper,
        int sxFloor,
        int syFloor,
        double sxFracFp,
        double syFracFp,
        //
        double[] tmpWxArr,
        PixelAccumulator pixelAccu) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        
        /*
         * Weight in x cached in array to avoid recomputation.
         * 
         * Caching wx in a 4*dxSpan length array
         * and wy in a 4 length array, both given to this method,
         * doesn't help (probably due to cache misses and memory load).
         */
        
        for (int kx = -1; kx <= 2; kx++) {
            final double wx = cubicWeight(kx - sxFracFp);
            tmpWxArr[kx + 1] = wx;
        }
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        
        pixelAccu.clearSum();
        
        // Iterate over 4x4 neighborhood.
        for (int ky = -1; ky <= 2; ky++) {
            final int sy = JisUtils.toRange(0, sh-1, syFloor + ky);
            final double wy = cubicWeight(ky - syFracFp);
            
            for (int kx = -1; kx <= 2; kx++) {
                final int sx = JisUtils.toRange(0, sw-1, sxFloor + kx);
                final double wx = tmpWxArr[kx + 1];
                
                final double w = wx * wy;
                
                final int srcPremulArgb32 =
                    srcHelper.getPremulArgb32At(sx, sy);
                pixelAccu.addPixelContrib(srcPremulArgb32, w);
            }
        }
        
        final int dstPremulArgb32 =
            pixelAccu.toValidPremulArgb32UnitDstSurf();
        
        return dstPremulArgb32;
    }
    
    /**
     * This weight can be negative.
     * 
     * @param x Its absolute value must be <= 2,
     *        which holds as long as we don't go further
     *        than +-2 around interpolation position.
     */
    private static double cubicWeight(double x) {
        /*
         * Can have accuracy tests to still pass
         * if using a 1024-long table of precomputed weights,
         * but perfs seem the same so not bothering.
         */
        x = Math.abs(x);
        final double x2 = x * x;
        final double ret;
        if (x <= 1.0) {
            ret = x2 * (x * (A + 2) - (A + 3)) + 1;
        } else {
            // Here we assume x <= 2.
            ret = A * (x2 * (x - 5) + 8 * x - 4);
        }
        return ret;
    }
}
