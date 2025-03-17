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
 * If downscaling divides a span by more than maxScaler2Downscaling,
 * first uses scaler1 to get this value down to maxScaler2Downscaling,
 * then uses scaler2 for the remaining scaling.
 */
public class ScalerPreDown1Then2 implements InterfaceScaler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double DEFAULT_MAX_SCALER2_DOWNSCALING = 2.0;
    
    private final InterfaceScaler scaler1;
    
    private final InterfaceScaler scaler2;
    
    private final double maxScaler2Downscaling;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * Uses 2.0 as maxScaler2Downscaling.
     */
    public ScalerPreDown1Then2(
        InterfaceScaler scaler1,
        InterfaceScaler scaler2) {
        this(
            scaler1,
            scaler2,
            DEFAULT_MAX_SCALER2_DOWNSCALING);
    }
    
    /**
     * @param scaler1 Scaler eventually used, for preliminary dowscaling.
     *        Must not be null.
     * @param scaler2 Scaler used for final scaling, unless there is
     *        no upscaling and maxScaler2Downscaling is close enough to 1
     *        than scaler2 does not need to be called.
     * @param maxScaler2Downscaling Max (srcSpan / dstSpan) ratio
     *        for downscaling with scaler2.
     *        Must be >= 1, 1 meaning any downscaling must be done
     *        with scaler1.
     * @throws NullPointerException if any scaler is null.
     * @throws IllegalArgumentException if maxStepDownscaling is not >= 1.0,
     *         or if scaler1 and scaler2 are the same, in which case
     *         this class has no point and it should just be used directly.
     */
    public ScalerPreDown1Then2(
        InterfaceScaler scaler1,
        InterfaceScaler scaler2,
        double maxScaler2Downscaling) {
        this.scaler1 = JisUtils.requireNonNull(scaler1);
        this.scaler2 = JisUtils.requireNonNull(scaler2);
        if (scaler1 == scaler2) {
            throw new IllegalArgumentException(
                "scalers must not be the same");
        }
        this.maxScaler2Downscaling =
            JisUtils.requireSupOrEq(
                1.0,
                maxScaler2Downscaling,
                "maxScaler2Downscaling");
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[pre=" + this.scaler1 + ",rem=" + this.scaler2 + "]";
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
        
        final int interWidth;
        final int interHeight;
        final boolean needScaler1PreDownscaling;
        {
            // Actual downscaling only if > 1.
            final double wDown = sw / (double) dw;
            final double hDown = sh / (double) dh;
            
            final double wDownForScaler2 = Math.min(this.maxScaler2Downscaling, wDown);
            final double hDownForScaler2 = Math.min(this.maxScaler2Downscaling, hDown);
            interWidth = JisUtils.roundToInt(dw * wDownForScaler2);
            interHeight = JisUtils.roundToInt(dh * hDownForScaler2);
            
            needScaler1PreDownscaling =
                (interWidth < sw)
                || (interHeight < sh);
        }
        
        final BufferedImageHelper finalSrcHelper;
        if (needScaler1PreDownscaling) {
            if ((interWidth == dw)
                && (interHeight == dh)) {
                // Only using scaler1.
                this.scaler1.scaleImage(
                    srcHelper,
                    dstHelper,
                    parallelExecutor);
                finalSrcHelper = null;
            } else {
                final BufferedImage tmpImage =
                    new BufferedImage(
                        interWidth,
                        interHeight,
                        BufferedImage.TYPE_INT_ARGB_PRE);
                final BufferedImageHelper tmpImageHelper =
                    new BufferedImageHelper(tmpImage);
                this.scaler1.scaleImage(
                    srcHelper,
                    tmpImageHelper,
                    parallelExecutor);
                finalSrcHelper = tmpImageHelper;
            }
        } else {
            finalSrcHelper = srcHelper;
        }
        
        if (finalSrcHelper != null) {
            this.scaler2.scaleImage(
                finalSrcHelper,
                dstHelper,
                parallelExecutor);
        }
    }
}
