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

public class ScalerNearestJis extends AbstractParallelScaler {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source area surface doesn't count (if ignoring memory read overhead).
     */
    private static final int SRC_AREA_THRESHOLD_FOR_SPLIT = Integer.MAX_VALUE;
    
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 2 * 1024;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int dstAreaThresholdForSplit;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerNearestJis() {
        this(DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT);
    }
    
    /**
     * @param dstAreaThresholdForSplit Must be >= 2.
     */
    public ScalerNearestJis(int dstAreaThresholdForSplit) {
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
    protected void scaleImagePart(
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
        
        // Optimization, to avoid useless scaling computations.
        final boolean gotXScaling = (dw != sw);
        final boolean gotYScaling = (dh != sh);
        
        final boolean dstPremul = dstImage.isAlphaPremultiplied();
        
        /*
         * 
         */
        
        final double di_to_0_1_factor = 1.0 / dw;
        final double dj_to_0_1_factor = 1.0 / dh;
        
        // Optimization, to avoid computing columns scaling for each colums.
        final int[] siByDicArr;
        if (gotXScaling) {
            siByDicArr = new int[dw];
            for (int di = 0; di < dw; di++) {
                final int si = computeSrcIndex(
                    sw,
                    di_to_0_1_factor,
                    di);
                siByDicArr[di] = si;
            }
        } else {
            // Src is already row-scaled.
            siByDicArr = null;
        }
        
        final int djStart = dstYStart;
        final int djEnd = djStart + (dstYEnd - dstYStart);
        
        // Optimization, to avoid retrieving source pixel
        // for each column of a same row.
        int currentSi = -1;
        int currentSj = -1;
        int currentArgb32 = 0;
        
        for (int dj = djStart; dj <= djEnd; dj++) {
            final int sj;
            if (gotYScaling) {
                sj = computeSrcIndex(
                    sh,
                    dj_to_0_1_factor,
                    dj);
            } else {
                sj = dj;
            }
            if (sj != currentSj) {
                // Invalidating currentSi,
                // for ARGB32 to be recomputed in next loop.
                currentSi = -1;
            }
            
            for (int di = 0; di < dw; di++) {
                final int si = (gotXScaling ? siByDicArr[di] : di);
                if (si != currentSi) {
                    /*
                     * For NEAREST, no interpolation,
                     * so no need to work in premul,
                     * and we can just use dstPremul format,
                     * which allows to avoid nonPremul->premul->nonPremul
                     * convesrion which can be slower and damage colors
                     * for low alpha.
                     */
                    currentArgb32 =
                        srcHelper.getArgb32At(si, sj, dstPremul);
                    currentSi = si;
                    currentSj = sj;
                }
                dstHelper.setArgb32At(di, dj, currentArgb32, dstPremul);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Computes the row or column index in source,
     * for the given row or column index in destination.
     * 
     * If there is no scaling, si = di,
     * so don't bother to call this.
     * 
     * @param srcLength Number of pixels in src.
     * @param dstLengthInv Inverse of the number of pixels in dst.
     * @return The index, from row left or column top, of the src column or row.
     */
    private static int computeSrcIndex(
        int srcLength,
        double dstLengthInv,
        int di) {
        
        /*
         * +-0.5 to account for the fact that pixels centers
         * coordinates are 0.5 away from pixels edges.
         */
        final double H = 0.5;
        final double ratio_0_1 = (di + H) * dstLengthInv;
        final double sid = ratio_0_1 * srcLength - H;
        final int si = JisUtils.roundToInt(sid);
        
        return si;
    }
}
