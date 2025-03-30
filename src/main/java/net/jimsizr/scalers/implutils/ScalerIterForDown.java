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
package net.jimsizr.scalers.implutils;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

/**
 * Iterates for downscaling to never divide span by more than two per step,
 * but does not iterate for upscaling.
 */
public class ScalerIterForDown implements InterfaceScaler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double DEFAULT_MAX_STEP_DOWNSCALING = 2.0;
    
    private final InterfaceScaler scaler;
    
    private final double maxStepDownscalingInv;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * Uses 2.0 as maxStepDownscaling.
     */
    public ScalerIterForDown(InterfaceScaler scaler) {
        this(
            scaler,
            DEFAULT_MAX_STEP_DOWNSCALING);
    }

    /**
     * @param scaler Must not be null.
     * @param maxStepDownscaling Must be > 1.0. Max downscaling for each step,
     *        unless this value is too close to 1 and would cause no scaling,
     *        in which case span is reduced by 1 even if it causes
     *        a larger downscaling.
     * @throws NullPointerException if scaler is null.
     * @throws IllegalArgumentException if maxStepDownscaling is not > 1.0.
     */
    public ScalerIterForDown(
        InterfaceScaler scaler,
        double maxStepDownscaling) {
        this.scaler = JisUtils.requireNonNull(scaler);
        JisUtils.requireSup(
            1.0,
            maxStepDownscaling,
            "maxStepDownscaling");
        // < 1.0
        this.maxStepDownscalingInv = 1.0 / maxStepDownscaling;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[iter=" + this.scaler + "]";
    }
    
    @Override
    public void scaleImage(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        BufferedImageHelper tmpSrcHelper = srcHelper;
        int tmpSw = sw;
        int tmpSh = sh;
        while (true) {
            final int tmpDw = this.computeDownscaledSpan(tmpSw, dw);
            final int tmpDh = this.computeDownscaledSpan(tmpSh, dh);
            if ((tmpDw <= dw)
                && (tmpDh <= dh)) {
                // We got too far down,
                // or just reached destination spans:
                // will finish with destination image.
                break;
            }
            
            /*
             * Not reusing intermediary image array to draw it into itself,
             * for it creates dynamic artifacts in case of parallelization,
             * and possibly harder to detect static ones in sequential case.
             * Not bothering to reuse array in case of 3 or more intermediary
             * images, because the third one should be relatively small
             * already (at most 1/8th of source image).
             */
            final BufferedImageHelper tmpDstHelper =
                JisImplUtils.newHelperAndImage(
                    tmpDw,
                    tmpDh,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            
            this.scaler.scaleImage(
                tmpSrcHelper,
                tmpDstHelper,
                parallelExecutor);
            
            // For next round or post-loop scaling.
            tmpSrcHelper = tmpDstHelper;
            tmpSw = tmpDw;
            tmpSh = tmpDh;
        }
        
        this.scaler.scaleImage(
            tmpSrcHelper,
            dstHelper,
            parallelExecutor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private int computeDownscaledSpan(int previousSpan, int dstSpan) {
        /*
         * Ceil to make sure span is never divided
         * by more than maxStepDownscaling,
         * unless factor is too close to 1
         * in which case we force downscaling.
         */
        int ret = Math.max(dstSpan, (int) Math.ceil(previousSpan * this.maxStepDownscalingInv));
        if ((ret > dstSpan) && (ret == previousSpan)) {
            // Did not downscale, but could: forcing downscaling.
            ret--;
        }
        return ret;
    }
}
