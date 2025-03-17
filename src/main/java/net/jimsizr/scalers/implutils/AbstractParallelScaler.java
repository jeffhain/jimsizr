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
                scaleImagePart(
                    this.cmn.srcHelper.duplicate(),
                    this.dstStartRow,
                    this.dstEndRow,
                    this.cmn.dstHelper.duplicate(),
                    this.cmn.runData);
            } finally {
                this.cmn.latch.countDown();
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    protected AbstractParallelScaler() {
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
        final int partCount;
        if (parallelExecutor != null) {
            final int srcAreaThreshold = this.getSrcAreaThresholdForSplit();
            final int dstAreaThreshold = this.getDstAreaThresholdForSplit();
            JisUtils.requireSupOrEq(2, srcAreaThreshold, "getSrcAreaThresholdForSplit()");
            JisUtils.requireSupOrEq(2, dstAreaThreshold, "getDstAreaThresholdForSplit()");
            
            // Areas always < Integer.MAX_VALUE, since it's a prime.
            final int srcArea = sw * sh;
            final int dstArea = dw * dh;
            
            final int partCountDueToSrc =
                JisUtils.toRange(1, dh,
                    (int) Math.ceil((srcArea + 1) / (double) srcAreaThreshold));
            final int partCountDueToDst =
                JisUtils.toRange(1, dh,
                    (int) Math.ceil((dstArea + 1) / (double) dstAreaThreshold));
            final int theoreticalPartCount = Math.max(partCountDueToSrc, partCountDueToDst);
            
            final int maxPartCount = CORE_COUNT * MAX_RUNNABLE_COUNT_PER_CORE;
            partCount = Math.min(maxPartCount, theoreticalPartCount);
        } else {
            partCount = 1;
        }
        
        final boolean prlElseSeq = (partCount >= 2);
        final Object runData = this.computeImplData(
            srcHelper,
            dstHelper,
            parallelExecutor,
            prlElseSeq);
        
        if (prlElseSeq) {
            parallelScale(
                partCount,
                srcHelper,
                dstHelper,
                parallelExecutor,
                runData);
        } else {
            final int dstYStart = 0;
            final int dstYEnd = dh - 1;
            this.scaleImagePart(
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
     * @return Object to give to scaleImagePart() calls.
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
     *        to corresponding scaleImagePart() calls. 
     */
    protected abstract void scaleImagePart(
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
     * @param partCount Always >= 2.
     * @param parallelExecutor Never null.
     */
    protected void parallelScale(
        int partCount,
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor,
        Object runData) {
        
        final int dh = dstHelper.getHeight();
        
        final Runnable[] runnableArr = new Runnable[partCount];
        
        final CountDownLatch latch =
            new CountDownLatch(partCount);
        
        final MyCmnData cmn =
            new MyCmnData(
                srcHelper,
                dstHelper,
                runData,
                latch);
        
        final double partHeightFp = dh / (double) partCount;
        
        int prevDstEndRowIndex = -1;
        for (int i = 0; i < partCount; i++) {
            final int dstStartRowIndex = prevDstEndRowIndex + 1;
            final double dstEndRowIndexFp = partHeightFp * i + (partHeightFp - 1.0);
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
