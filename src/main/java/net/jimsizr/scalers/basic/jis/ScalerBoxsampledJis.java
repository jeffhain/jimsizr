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

public class ScalerBoxsampledJis extends AbstractParallelScaler {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source pixels are iterated over more efficiently than destination pixels,
     * for which source coverage computation machinery takes place,
     * so we use a larger threshold for these. 
     */
    private static final int DEFAULT_SRC_AREA_THRESHOLD_FOR_SPLIT = 1024;
    
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 512;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double H = 0.5;
    
    /**
     * Ignoring src pixels which less than an epsilon is covered.
     * Allows to rule out approximation error related edge cases
     * that would take us out of src area, without adding
     * bounds checks overhead.
     * 
     * We consider the following worst case: src is a (1*1) square,
     * and dst is a (n*(n+1)) rectangle with n = sqrt(Integer.MAX_VALUE)
     * (doesn't overflow).
     * Each dst pixel will cover only 1/(n*(n+1)) of src pixel,
     * but we don't want to ignore src contribution.
     * We therefore choose epsilon = 1.0/Integer.MAX_VALUE
     * = 4.656612875245797E-10 (which is < 1/(n*(n+1))).
     */
    private static final double PIXEL_RATIO_EPSILON = 1.0 / Integer.MAX_VALUE;
    
    /*
     * 
     */
    
    private final int srcAreaThresholdForSplit;
    private final int dstAreaThresholdForSplit;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerBoxsampledJis() {
        this(
            DEFAULT_SRC_AREA_THRESHOLD_FOR_SPLIT,
            DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT);
    }
    
    /**
     * @param srcAreaThresholdForSplit Must be >= 2.
     * @param dstAreaThresholdForSplit Must be >= 2.
     */
    public ScalerBoxsampledJis(
        int srcAreaThresholdForSplit,
        int dstAreaThresholdForSplit) {
        this.srcAreaThresholdForSplit =
            JisUtils.requireSupOrEq(2, srcAreaThresholdForSplit, "srcAreaThresholdForSplit");
        this.dstAreaThresholdForSplit =
            JisUtils.requireSupOrEq(2, dstAreaThresholdForSplit, "dstAreaThresholdForSplit");
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName()
            + ",srcSplit=" + this.srcAreaThresholdForSplit
            + ",dstSplit=" + this.dstAreaThresholdForSplit
            + "]";
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getSrcAreaThresholdForSplit() {
        return this.srcAreaThresholdForSplit;
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
        
        if (isAlignedShrinking(sw, sh, dw, dh)) {
            scaleImageChunk_boxsampled_alignedShrinking(
                srcHelper,
                //
                dstYStart,
                dstYEnd,
                dstHelper);
        } else {
            scaleImageChunk_boxsampled_general(
                srcHelper,
                //
                dstYStart,
                dstYEnd,
                dstHelper);
        }
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Package-private for tests.
     * 
     * @return Premul destination color.
     */
    static int computeBoxsampledColor32_general(
        BufferedImageHelper srcHelper,
        double centerXFp,
        double centerYFp,
        double dxPixelSpanFp,
        //
        double clpYSpanFp,
        double loYRatio,
        int fullDyMin,
        int fullDyMax,
        double hiYRatio,
        //
        PixelAccumulator pixelAccu) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double xaFp = -H;
        final double xbFp = sw - H; // xMax + 0.5
        
        // Clamping.
        final double clpXMinFp = JisUtils.toRange(xaFp, xbFp, centerXFp - dxPixelSpanFp * H);
        final double clpXMaxFp = JisUtils.toRange(xaFp, xbFp, centerXFp + dxPixelSpanFp * H);
        //
        final double clpXSpanFp = clpXMaxFp - clpXMinFp;
        
        final double dstPixelSurfInSrcInv = 1.0 / (clpXSpanFp * clpYSpanFp);
        if (dstPixelSurfInSrcInv == Double.POSITIVE_INFINITY) {
            /*
             * Tiny surface.
             * Can't happen due to pixel spans, which are never tiny enough,
             * but due to center being out of clip (if we add clipping).
             */
            final int srcX = JisUtils.toRange(0, sw - 1,
                JisUtils.roundToInt(centerXFp));
            final int srcY = JisUtils.toRange(0, sh - 1,
                JisUtils.roundToInt(centerYFp));
            return srcHelper.getPremulArgb32At(srcX, srcY);
        }
        
        final double clpDxMinFp = (clpXMinFp - xaFp);
        final double clpDxMaxFp = (clpXMaxFp - xaFp);
        
        final int clpDxMinFloor = (int) clpDxMinFp;
        final int clpDxMaxFloor = (int) clpDxMaxFp;
        
        // When no pixel is fully covered in X,
        // fullDxMin is the coordinate of the pixel after the one covered,
        // so as to always have loXRatio used for pixel at fullDxMin-1.
        final double loXRatio;
        final int fullDxMin;
        final int fullDxMax;
        final double hiXRatio;
        if ((clpDxMinFloor == clpDxMaxFloor)
            || (clpDxMinFloor + 1.0 == clpDxMaxFp)) {
            /*
             * Area in same X column.
             * Will only use (loXRatio,fullDxMin-1).
             */
            loXRatio = (clpDxMaxFp - clpDxMinFp);
            fullDxMin = clpDxMinFloor + 1;
            fullDxMax = Integer.MIN_VALUE;
            hiXRatio = 0.0;
        } else {
            /*
             * Area over at least two X columns.
             */
            loXRatio = (clpDxMinFloor + 1 - clpDxMinFp);
            fullDxMin = clpDxMinFloor + 1;
            fullDxMax = clpDxMaxFloor - 1;
            hiXRatio = (clpDxMaxFp - clpDxMaxFloor);
        }
        
        pixelAccu.clearSum();
        
        if (loYRatio > PIXEL_RATIO_EPSILON) {
            // Top side.
            final int srcY = fullDyMin - 1;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                // Top-left corner.
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMin - 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, loXRatio * loYRatio);
            }
            {
                // Top side central.
                for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                    final int srcColor32 =
                        srcHelper.getPremulArgb32At(ki, srcY);
                    pixelAccu.addPixelContrib(srcColor32, loYRatio);
                }
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                // Top-right corner.
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMax + 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, hiXRatio * loYRatio);
            }
        }
        // Horizontal central.
        for (int kj = fullDyMin; kj <= fullDyMax; kj++) {
            final int srcY = kj;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMin - 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, loXRatio);
            }
            // This is where we spend the most time
            // for big downscalings: taking care for this loop
            // be over x and not y.
            for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(ki, srcY);
                pixelAccu.addFullPixelContrib(srcColor32);
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMax + 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, hiXRatio);
            }
        }
        if (hiYRatio > PIXEL_RATIO_EPSILON) {
            // Bottom side.
            final int srcY = fullDyMax + 1;
            if (loXRatio > PIXEL_RATIO_EPSILON) {
                // Bottom-left corner.
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMin - 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, loXRatio * hiYRatio);
            }
            {
                // Bottom side central.
                for (int ki = fullDxMin; ki <= fullDxMax; ki++) {
                    final int srcColor32 =
                        srcHelper.getPremulArgb32At(ki, srcY);
                    pixelAccu.addPixelContrib(srcColor32, hiYRatio);
                }
            }
            if (hiXRatio > PIXEL_RATIO_EPSILON) {
                // Bottom-right corner.
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(fullDxMax + 1, srcY);
                pixelAccu.addPixelContrib(srcColor32, hiXRatio * hiYRatio);
            }
        }
        
        final int dstPremulColor32 =
            pixelAccu.toPremulArgb32(
                dstPixelSurfInSrcInv);
        
        return dstPremulColor32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean isAlignedShrinking(
        int sw,
        int sh,
        int dw,
        int dh) {
        return (sw % dw == 0)
            && (sh % dh == 0);
    }
    
    /*
     * 
     */
    
    /**
     * sr spans must be multiples of dr spans.
     * 
     * About 10-30 percents performance gain
     * over the general sampling method.
     * 
     * Rectangles emptiness checks are supposed already done
     * (must not be a public method).
     */
    private static void scaleImageChunk_boxsampled_alignedShrinking(
        BufferedImageHelper srcHelper,
        //
        int dstYStart,
        int dstYEnd,
        BufferedImageHelper dstHelper) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        // dst pixel width in src pixels,
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int dxPixelSpan = sw / dw;
        // dst pixel height in src pixels,
        // when scaled up to match its corresponding src pixels.
        // Exact division.
        final int dyPixelSpan = sh / dh;
        
        final PixelAccumulator pixelAccu = new PixelAccumulator();
        
        final double dstPixelSurfInSrc = (dxPixelSpan * (double) dyPixelSpan);
        final double dstPixelSurfInSrcInv = 1.0 / dstPixelSurfInSrc;
        
        final int djStart = dstYStart;
        final int djEnd = djStart + (dstYEnd - dstYStart);
        // Looping on destination pixels.
        for (int dj = djStart; dj <= djEnd; dj++) {
            final int srcYOffset = dj * dyPixelSpan;
            for (int di = 0; di < dw; di++) {
                final int srcXOffset = di * dxPixelSpan;
                final int dstPremulArgb32 =
                    computeBoxsampledColor32_alignedShrinking(
                        srcHelper,
                        dstPixelSurfInSrcInv,
                        srcXOffset,
                        srcYOffset,
                        dxPixelSpan,
                        dyPixelSpan,
                        //
                        pixelAccu);
                dstHelper.setPremulArgb32At(
                    di,
                    dj,
                    dstPremulArgb32);
            }
        }
    }
    
    private static int computeBoxsampledColor32_alignedShrinking(
        BufferedImageHelper srcHelper,
        double dstPixelSurfInSrcInv,
        int srcXOffset,
        int srcYOffset,
        int dxPixelSpan,
        int dyPixelSpan,
        //
        PixelAccumulator pixelAccu) {
        
        pixelAccu.clearSum();
        
        for (int kj = 0; kj < dyPixelSpan; kj++) {
            final int srcY = srcYOffset + kj;
            for (int ki = 0; ki < dxPixelSpan; ki++) {
                final int srcX = srcXOffset + ki;
                final int srcColor32 =
                    srcHelper.getPremulArgb32At(srcX, srcY);
                pixelAccu.addFullPixelContrib(srcColor32);
            }
        }
        
        final int dstPremulColor32 =
            pixelAccu.toPremulArgb32(
                dstPixelSurfInSrcInv);
        
        return dstPremulColor32;
    }
    
    /**
     * Rectangles emptiness checks are supposed already done
     * (must not be a public method).
     */
    private static void scaleImageChunk_boxsampled_general(
        BufferedImageHelper srcHelper,
        //
        int dstYStart,
        int dstYEnd,
        BufferedImageHelper dstHelper) {
        
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
        
        final PixelAccumulator pixelAccu = new PixelAccumulator();
        
        // Reference for coordinates deltas.
        // Makes intersections with pixels borders easier to compute.
        final double yaFp = -H;
        final double ybFp = sh - H; // yMax + 0.5
        
        // +-0.5 needed due to integer coordinates
        // corresponding to pixels centers.
        final double H = 0.5;
        final int djStart = dstYStart;
        final int djEnd = djStart + (dstYEnd - dstYStart);
        // Loop on srcY and inside on srcX (memory-friendly).
        for (int dj = djStart; dj <= djEnd; dj++) {
            // y in src of destination pixel's center. 
            final double centerYFp = (dj + H) * dyPixelSpanFp - H;
            
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
            
            for (int di = 0; di < dw; di++) {
                // x in src of destination pixel's center. 
                final double centerXFp = (di + H) * dxPixelSpanFp - H;
                final int dstPremulArgb32 =
                    computeBoxsampledColor32_general(
                        srcHelper,
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
                dstHelper.setPremulArgb32At(
                    di,
                    dj,
                    dstPremulArgb32);
            }
        }
    }
}
