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
package net.jimsizr.scalers.basic.awt;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import net.jimsizr.scalers.implutils.AbstractParallelScaler;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

public class BaseScalerAwt extends AbstractParallelScaler {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source area surface doesn't count (if ignoring memory read overhead).
     */
    private static final int SRC_AREA_THRESHOLD_FOR_SPLIT = Integer.MAX_VALUE;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Object renderingHint;
    
    private final int dstAreaThresholdForSplit;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * @param dstAreaThresholdForSplit Must be >= 2.
     */
    public BaseScalerAwt(
        Object renderingHint,
        int dstAreaThresholdForSplit) {
        this.renderingHint = renderingHint;
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
    // PROTECTED
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
        
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        // We assume we can do that concurrently,
        // which seems to work.
        final Graphics2D g = this.createGraphicsForScaling(dstImage);
        try {
            if ((dstYStart != 0)
                || (dstYEnd != dh - 1)) {
                // This clip is what allows parallel work chunks
                // not to step on each other's toes.
                g.setClip(
                    0,
                    dstYStart,
                    dw,
                    (dstYEnd - dstYStart + 1));
            }
            
            final ImageObserver observer = null;
            g.drawImage(
                srcImage,
                0, // x
                0, // y
                dw, // width
                dh, // height
                observer);
        } finally {
            g.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private Graphics2D createGraphicsForScaling(BufferedImage image) {
        final Graphics2D g = image.createGraphics();
        /*
         * In case source has alpha.
         * Always works, because even though we might use clips,
         * (srcX,srcY) is always (0,0).
         */
        g.setComposite(AlphaComposite.Src);
        /*
         * 
         */
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            this.renderingHint);
        return g;
    }
}
