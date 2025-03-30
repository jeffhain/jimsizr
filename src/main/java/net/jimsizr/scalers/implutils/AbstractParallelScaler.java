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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisPrlEngine;
import net.jimsizr.utils.JisUtils;

/**
 * Abstract class for implementing parallel scalers.
 * 
 * Eventual delegation to functionally equivalent scaling
 * (like NEAREST if spans don't change) should be done before
 * calling these scalers, not within their abstract scaling method,
 * for proper split thresholds to be applied.
 */
public abstract class AbstractParallelScaler implements InterfaceScaler {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * To avoid over-splitting, since we don't use
     * a split-as-needed parallelization engine.
     * 
     * Must not be too small else work will not be cut into small enough chunks
     * to keep all workers busy until near completion.
     * Must not be too large else might split into more chunks than needed
     * and add useless overhead.
     * 
     * Our work load being split into approximately equal amounts,
     * a value of 10 should allow an execution time usually not larger
     * than "theoretical optimal * 1.1", with 1.1 = 1 + 1/10.
     */
    private static final int MAX_RUNNABLE_COUNT_PER_CORE = 10;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyCmnData {
        final BufferedImageHelper srcHelper;
        final BufferedImageHelper dstHelper;
        final Object runData;
        final CountDownLatch latch;
        public MyCmnData(
            BufferedImageHelper srcHelper,
            BufferedImageHelper dstHelper,
            Object runData,
            CountDownLatch latch) {
            this.srcHelper = srcHelper;
            this.dstHelper = dstHelper;
            this.runData = runData;
            this.latch = latch;
        }
    }
    
    private class MyPrlRunnable implements Runnable {
        private final MyCmnData cmn;
        private final int dstStartRow;
        private final int dstEndRow;
        public MyPrlRunnable(
            MyCmnData cmn,
            int dstStartRow,
            int dstEndRow) {
            if (dstStartRow > dstEndRow) {
                throw new IllegalArgumentException(
                    dstStartRow + " > " + dstEndRow);
            }
            if (dstEndRow >= cmn.dstHelper.getHeight()) {
                throw new IllegalArgumentException(
                    dstEndRow + " >= " + cmn.dstHelper.getHeight());
            }
            this.cmn = cmn;
            this.dstStartRow = dstStartRow;
            this.dstEndRow = dstEndRow;
        }
        @Override
        public void run() {
            try {
                scaleImageChunk(
                    duplicateOrSame(this.cmn.srcHelper),
                    this.dstStartRow,
                    this.dstEndRow,
                    duplicateOrSame(this.cmn.dstHelper),
                    this.cmn.runData);
            } finally {
                this.cmn.latch.countDown();
            }
        }
        private BufferedImageHelper duplicateOrSame(BufferedImageHelper helper) {
            final BufferedImageHelper ret;
            if (mustDuplicateHelpersForChunks) {
                ret = helper.duplicate();
            } else {
                ret = helper;
            }
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    
    private final boolean mustDuplicateHelpersForChunks;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * Uses true for mustDuplicateHelpersForChunks.
     */
    protected AbstractParallelScaler() {
        this(true);
    }
    
    /**
     * @param mustDuplicateHelpersForChunks Can be set to false if not using
     *        helpers non-thread-safe methods in scaleImageChunk(),
     *        to avoid useless calls to BufferedImageHelper.duplicate().
     */
    protected AbstractParallelScaler(boolean mustDuplicateHelpersForChunks) {
        this.mustDuplicateHelpersForChunks = mustDuplicateHelpersForChunks;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "]";
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
        
        /*
         * Images spans can be assumed not to be zero,
         * thanks to SampleModel constructor check.
         */
        
        // 1 when going sequential, 2 or more when going parallel.
        final int chunkCount;
        if (parallelExecutor != null) {
            final int srcAreaThreshold = this.getSrcAreaThresholdForSplit();
            final int dstAreaThreshold = this.getDstAreaThresholdForSplit();
            JisUtils.requireSupOrEq(2, srcAreaThreshold, "getSrcAreaThresholdForSplit()");
            JisUtils.requireSupOrEq(2, dstAreaThreshold, "getDstAreaThresholdForSplit()");
            
            // Areas always < Integer.MAX_VALUE, since it's a prime.
            final int srcArea = sw * sh;
            final int dstArea = dw * dh;
            
            final int chunkCountDueToSrc =
                JisUtils.toRange(1, dh,
                    (int) Math.ceil((srcArea + 1) / (double) srcAreaThreshold));
            final int chunkCountDueToDst =
                JisUtils.toRange(1, dh,
                    (int) Math.ceil((dstArea + 1) / (double) dstAreaThreshold));
            final int theoreticalChunkCount = Math.max(chunkCountDueToSrc, chunkCountDueToDst);
            
            final int maxChunkCount = CORE_COUNT * MAX_RUNNABLE_COUNT_PER_CORE;
            chunkCount = Math.min(maxChunkCount, theoreticalChunkCount);
        } else {
            chunkCount = 1;
        }
        
        final boolean prlElseSeq = (chunkCount >= 2);
        final Object runData = this.computeImplData(
            srcHelper,
            dstHelper,
            parallelExecutor,
            prlElseSeq);
        
        if (prlElseSeq) {
            parallelScale(
                chunkCount,
                srcHelper,
                dstHelper,
                parallelExecutor,
                runData);
        } else {
            final int dstYStart = 0;
            final int dstYEnd = dh - 1;
            this.scaleImageChunk(
                srcHelper,
                //
                dstYStart,
                dstYEnd,
                dstHelper,
                //
                runData);
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For no parallelization, return Integer.MAX_VALUE
     * for both thresholds (it's a prime, so no int product
     * of width * height can be equal to it).
     * 
     * @return The source area, in number of source pixels,
     *         from which it's worth to split in two for parallelization.
     *         Must be >= 2.
     */
    protected abstract int getSrcAreaThresholdForSplit();
    
    /**
     * For no parallelization, return Integer.MAX_VALUE
     * for both thresholds (it's a prime, so no int product
     * of width * height can be equal to it).
     * 
     * @return The destination area, in number of destination pixels,
     *         from which it's worth to split in two for parallelization.
     *         Must be >= 2.
     */
    protected abstract int getDstAreaThresholdForSplit();
    
    /**
     * This default implementation returns null.
     * 
     * Called on each call to scaleImage(), with its arguments.
     * 
     * @param prlElseSeq True if going parallel, false otherwise.
     * @return Object to give to scaleImageChunk() calls.
     */
    protected Object computeImplData(
        @SuppressWarnings("unused")
        BufferedImageHelper srcHelper,
        @SuppressWarnings("unused")
        BufferedImageHelper dstHelper,
        @SuppressWarnings("unused")
        Executor parallelExecutor,
        boolean prlElseSeq) {
        return null;
    }
    
    /**
     * @param srcHelper Source image helper.
     * @param dstYStart Destination "y" to start from (inclusive).
     * @param dstYEnd Destination "y" to end at (inclusive).
     * @param dstHelper Destination image helper.
     * @param runData Data (anything, up to the scaler implementation)
     *        that can be given by each scaleImage() call
     *        to corresponding scaleImageChunk() calls. 
     */
    protected abstract void scaleImageChunk(
        BufferedImageHelper srcHelper,
        //
        int dstYStart,
        int dstYEnd,
        BufferedImageHelper dstHelper,
        //
        Object runData);
    
    /*
     * 
     */
    
    /**
     * Called in case of parallel scaling.
     * 
     * @param chunkCount Always >= 2.
     * @param parallelExecutor Never null.
     */
    protected void parallelScale(
        int chunkCount,
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor,
        Object runData) {
        
        final int dh = dstHelper.getHeight();
        
        final Runnable[] runnableArr = new Runnable[chunkCount];
        
        final CountDownLatch latch =
            new CountDownLatch(chunkCount);
        
        final MyCmnData cmn =
            new MyCmnData(
                srcHelper,
                dstHelper,
                runData,
                latch);
        
        final double chunkHeightFp = dh / (double) chunkCount;
        
        int prevDstEndRowIndex = -1;
        for (int i = 0; i < chunkCount; i++) {
            final int dstStartRowIndex = prevDstEndRowIndex + 1;
            final double dstEndRowIndexFp = chunkHeightFp * i + (chunkHeightFp - 1.0);
            final int dstEndRowIndex = (int) (dstEndRowIndexFp + 0.5);
            runnableArr[i] = new MyPrlRunnable(
                cmn,
                dstStartRowIndex,
                dstEndRowIndex);
            prevDstEndRowIndex = dstEndRowIndex;
        }
        if (prevDstEndRowIndex != dh - 1) {
            // Means our algo doesn't go to last row.
            throw new AssertionError();
        }
        
        JisPrlEngine.parallelRun(
            runnableArr,
            parallelExecutor,
            latch);
    }
}
