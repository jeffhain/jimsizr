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
package net.jimsizr.scalers.smart.copy;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.implutils.AbstractParallelScaler;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyInterKinds;
import net.jimsizr.types.InterKind;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

/**
 * Uses whatever is the fastest.
 */
public class ScalerCopySmart extends AbstractParallelScaler {
    
    /*
     * NB: When destination is binary, parallel can easily be slower
     * than sequential, due to use of some lock in binary images,
     * but going parallel can also be a bit faster due to some
     * parallel work happening outside of this lock,
     * so to keep things simple we don't bother to do a final
     * sequential copy into destination binary image.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Source area surface doesn't count (if ignoring memory read overhead).
     */
    private static final int SRC_AREA_THRESHOLD_FOR_SPLIT = Integer.MAX_VALUE;
    
    /**
     * Large enough to avoid much slow down in case of fast copy (the usual case),
     * but not more.
     */
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 32 * 1024;
    
    /**
     * In case of intermediary images, copying sequentially slice after slice
     * helps due to both less allocation (one small image of each type,
     * reused through slices, rather than one big of each type),
     * and less cache misses (small images staying somewhat in cache).
     */
    private static final int DEFAULT_MAX_SLICE_AREA = 16 * 1024;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int dstAreaThresholdForSplit;
    
    private final int maxSliceArea;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerCopySmart() {
        this(
            DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT,
            DEFAULT_MAX_SLICE_AREA);
    }
    
    /**
     * @param dstAreaThresholdForSplit Must be >= 2.
     * @param maxSliceArea Must be >= 1.
     */
    public ScalerCopySmart(
        int dstAreaThresholdForSplit,
        int maxSliceArea) {
        this.dstAreaThresholdForSplit =
            JisUtils.requireSupOrEq(2, dstAreaThresholdForSplit, "dstAreaThresholdForSplit");
        this.maxSliceArea =
            JisUtils.requireSupOrEq(1, maxSliceArea, "maxSliceArea");
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName()
            + ",dstSplit=" + this.dstAreaThresholdForSplit
            + ",maxSlice=" + this.maxSliceArea
            + "]";
    }
    
    @Override
    public void scaleImage(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor) {
        
        JisUtils.checkNoScaling(
            srcHelper.getImage(),
            dstHelper.getImage());
        
        super.scaleImage(
            srcHelper,
            dstHelper,
            parallelExecutor);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Overridable for tuning or using better algos
     * depending on drawImage() platform-specificities.
     */
    protected CopyAlgo getCopyAlgo(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        boolean prlElseSeq) {
        
        return BestCopyAlgoHelper.getBestCopyAlgo(
            srcHelper.getImageType(),
            srcHelper.isAlphaPremultiplied(),
            dstHelper.getImageType(),
            dstHelper.isAlphaPremultiplied(),
            prlElseSeq);
    }
    
    @Override
    protected int getSrcAreaThresholdForSplit() {
        return SRC_AREA_THRESHOLD_FOR_SPLIT;
    }
    
    @Override
    protected int getDstAreaThresholdForSplit() {
        return this.dstAreaThresholdForSplit;
    }
    
    /**
     * @return {AlgoBrand,preImageTypeList}
     */
    @Override
    protected Object computeImplData(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor,
        boolean prlElseSeq) {
        
        final CopyAlgo copyAlgo =
            this.getCopyAlgo(
                srcHelper,
                dstHelper,
                prlElseSeq);
        final CopyInterKinds interKinds = copyAlgo.getInterKinds();
        
        final List<Integer> preImageTypeList =
            new ArrayList<Integer>(interKinds.interKindCount());
        for (InterKind interKind : interKinds.interKindList()) {
            preImageTypeList.add(interKind.getImageType());
        }
        return new Object[] {copyAlgo.getBrand(), preImageTypeList};
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
        
        final int height = (dstYEnd - dstYStart + 1);
        
        final Object[] brandAndPreArr = (Object[]) runData;
        final AlgoBrand algoBrand = (AlgoBrand) brandAndPreArr[0];
        @SuppressWarnings("unchecked")
        final List<Integer> preImageTypeList = (List<Integer>) brandAndPreArr[1];
        
        this.copyImage_prlChunk(
            srcHelper,
            dstHelper,
            //
            dstYStart,
            height,
            //
            algoBrand,
            preImageTypeList);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void copyImage_prlChunk(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        int yStart,
        int height,
        //
        AlgoBrand algoBrand,
        List<Integer> preImageTypeList) {
        
        final int preCount = preImageTypeList.size();
        final boolean gotTmpImages = (preCount >= 1);
        if (gotTmpImages) {
            final int width = srcHelper.getWidth();

            /*
             * sliceMaxArea = width * sliceMaxHeight
             * sliceMaxHeight ~= sliceMaxArea / width
             */
            final int sliceMaxHeight = (1 + (this.maxSliceArea - 1) / width);
            
            final int maxTmpImageType = getMax(preImageTypeList);
            // Temporary images in intermediary formats,
            // also reused for drawing each new slice.
            final BufferedImage[] tmpImageByType = new BufferedImage[maxTmpImageType + 1];
            // Temporary images graphics,
            // also reused for drawing each new slice.
            final Graphics2D[] tmpGByType = new Graphics2D[maxTmpImageType + 1];
            // For JIS.
            final BufferedImageHelper[] tmpLastSrcHelperHolder =
                ((algoBrand == AlgoBrand.JIS)
                    ? new BufferedImageHelper[1] : null);
            
            // Possibly one (so no actual slicing).
            final int sliceCount = 1 + ((height - 1) / sliceMaxHeight);
            int offset = 0;
            for (int i = 0; i < sliceCount; i++) {
                final int sliceHeight = Math.min(height - offset, sliceMaxHeight);
                final int sliceYStart = yStart + offset;
                copyImage_prlChunk_slicedOrNot(
                    srcHelper,
                    dstHelper,
                    //
                    sliceYStart,
                    sliceHeight,
                    //
                    algoBrand,
                    preImageTypeList,
                    tmpImageByType,
                    tmpGByType,
                    tmpLastSrcHelperHolder);
                offset += sliceHeight;
            }
            for (Graphics2D tmpG : tmpGByType) {
                if (tmpG != null) {
                    tmpG.dispose();
                }
            }
        } else {
            copyImage_prlChunk_slicedOrNot(
                srcHelper,
                dstHelper,
                //
                yStart,
                height,
                //
                algoBrand,
                preImageTypeList,
                null,
                null,
                null);
        }
    }
    
    private static void copyImage_prlChunk_slicedOrNot(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        int yStart,
        int height,
        //
        AlgoBrand algoBrand,
        List<Integer> preImageTypeList,
        BufferedImage[] tmpImageByType,
        Graphics2D[] tmpGByType,
        BufferedImageHelper[] tmpLastSrcHelperHolder) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int width = srcImage.getWidth();
        
        BufferedImage lastSrcImage = srcImage;
        int lastSrcYStart = yStart;
        final int preCount = preImageTypeList.size();
        
        /*
         * Preliminary copies, for whole image or just
         * for one parallelized chunk, or just a slice of them
         * (if having preliminary intermediary images):
         * drawing on temporary images.
         */
        
        for (int i = 0; i < preCount; i++) {
            final int tmpDstImageType = preImageTypeList.get(i);
            BufferedImage tmpDstImage;
            Graphics2D tmpG = tmpGByType[tmpDstImageType];
            if (tmpG == null) {
                /*
                 * In case of multiple slices, each image is allocated
                 * using the first slice, so its height is also good
                 * for the last slice, which is the only one that
                 * can be smaller than other slices.
                 */
                tmpDstImage = new BufferedImage(
                    width,
                    height,
                    tmpDstImageType);
                tmpG = createGraphicsForCopy(tmpDstImage);
                
                tmpImageByType[tmpDstImageType] = tmpDstImage;
                tmpGByType[tmpDstImageType] = tmpG;
            } else {
                tmpDstImage = tmpImageByType[tmpDstImageType];
            }
            tmpG.drawImage(
                lastSrcImage,
                //
                0, // dx1
                0, // dy1
                width, // dx2 (exclusive)
                height, // dy2 (exclusive)
                //
                0, // sx1
                lastSrcYStart, // sy1
                width, // sx2 (exclusive)
                lastSrcYStart + height, // sy2 (exclusive)
                //
                null);
            
            lastSrcImage = tmpDstImage;
            lastSrcYStart = 0;
        }
        
        /*
         * Last (or single) copy, for whole image or just
         * for one parallelized chunk, or just a slice of them
         * (if having preliminary intermediary images):
         * using brand's algo and drawing on destination image.
         * 
         * Can't use InterfaceScaler API here,
         * since we might be copying just a chunk or a slice.
         */
        
        if (algoBrand == AlgoBrand.AWT) {
            final Graphics2D g = createGraphicsForCopy(dstImage);
            try {
                g.drawImage(
                    lastSrcImage,
                    //
                    0, // dx1
                    yStart, // dy1
                    width, // dx2 (exclusive)
                    yStart + height, // dy2 (exclusive)
                    //
                    0, // sx1
                    lastSrcYStart, // sy1
                    width, // sx2 (exclusive)
                    lastSrcYStart + height, // sy2 (exclusive)
                    //
                    null);
            } finally {
                g.dispose();
            }
        } else if (algoBrand == AlgoBrand.JIS) {
            final BufferedImageHelper lastSrcHelper;
            if (lastSrcImage == srcImage) {
                lastSrcHelper = srcHelper;
            } else {
                BufferedImageHelper lsh = tmpLastSrcHelperHolder[0];
                if (lsh == null) {
                    lsh = new BufferedImageHelper(lastSrcImage);
                    tmpLastSrcHelperHolder[0] = lsh;
                }
                lastSrcHelper = lsh;
            }
            BufferedImageHelper.copyImage(
                lastSrcHelper,
                0,
                lastSrcYStart,
                //
                dstHelper,
                0,
                yStart,
                //
                width,
                height);
        } else {
            throw new IllegalArgumentException("" + algoBrand);
        }
    }
    
    private static Graphics2D createGraphicsForCopy(BufferedImage image) {
        final Graphics2D g = image.createGraphics();
        /*
         * Only reliable if no more than two
         * of (srcX,srcY,dstX,dstY) are zero,
         * which is not always the case here,
         * but the problematic cases are ruled out
         * at smart code generation due to not being
         * accurate.
         */
        g.setComposite(AlphaComposite.Src);
        /*
         * In case not the default.
         */
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        return g;
    }
    
    private static int getMax(List<Integer> list) {
        int ret = Integer.MIN_VALUE;
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            ret = Math.max(ret, list.get(i));
        }
        return ret;
    }
}
