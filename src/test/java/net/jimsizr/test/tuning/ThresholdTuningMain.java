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
package net.jimsizr.test.tuning;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.scalers.basic.awt.ScalerBicubicAwt;
import net.jimsizr.scalers.basic.awt.ScalerBilinearAwt;
import net.jimsizr.scalers.basic.awt.ScalerNearestAwt;
import net.jimsizr.scalers.basic.jis.ScalerBicubicJis;
import net.jimsizr.scalers.basic.jis.ScalerBilinearJis;
import net.jimsizr.scalers.basic.jis.ScalerBoxsampledJis;
import net.jimsizr.scalers.basic.jis.ScalerNearestJis;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * To tune split thresholds.
 * 
 * Tuning thresholds for the normally fastest case
 * (opaque, premul ARGB32, etc.),
 * for best perfs when using it
 * (not bothering with more adaptive thresholds).
 */
public class ThresholdTuningMain {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean MUST_LOG_DETAILS = true;
    
    private static final boolean MUST_TUNE_AWT = true;
    private static final boolean MUST_TUNE_JIS = true;
    
    /**
     * Preferring smaller thresholds, which allow for parallelism
     * with small images, as long as our measure for them
     * is up to 10 percents slower.
     */
    private static final double ALLOWED_SLOWER_FACTOR = 1.1;

    private static final int WARMUP_CALL_COUNT = 5;
    
    private static final int NBR_OF_TIMINGS = 10;
    
    /**
     * Dividing by two because hyper-threading is not that good.
     * 
     * Not too large else causes too large images in near-thresholds benches,
     * and memory becomes bottleneck for too large parallelism.
     */
    private static final int MAX_PRL =
        Math.min(
            16,
            Runtime.getRuntime().availableProcessors() / 2);
    
    private static final int MIN_PRL = Math.min(2, MAX_PRL);
    
    /**
     * Matters to have small images,
     * to see how small thresholds perform,
     * but not too small, else don't get parallelized
     * and biases our heuristic.
     * 
     * NB: Small thresholds not applied to large images,
     * because can't fulfill them due to (larger) rows
     * not being split (only splitting between rows).
     */
    private static final int[] DST_SPAN_ARR = new int[] {
        256,
        512,
        1024,
        2048,
    };
    
    private static final int[] THRESHOLD_ARR = new int[] {
        512, // 512/2 = 256 (min dst span, so min dst width)
        1024,
        2 * 1024,
        4 * 1024,
        16 * 1024,
        32 * 1024,
        64 * 1024,
    };
    
    private static final int MAX_DST_SPAN = DST_SPAN_ARR[DST_SPAN_ARR.length - 1];
    private static final int MAX_DST_AREA = MAX_DST_SPAN * MAX_DST_SPAN;
    
    private static List<InterfaceScalerFactoryForTest> newScalerFactoryList() {
        
        final List<InterfaceScalerFactoryForTest> ret = new ArrayList<>();
        
        if (MUST_TUNE_AWT) {
            ret.add(new MyScalerFactoryNearestAwt());
            ret.add(new MyScalerFactoryBilinearAwt());
            ret.add(new MyScalerFactoryBicubicAwt());
        }
        
        if (MUST_TUNE_JIS) {
            ret.add(new MyScalerFactoryNearestJis());
            ret.add(new MyScalerFactoryBilinearJis());
            ret.add(new MyScalerFactoryBicubicJis());
            ret.add(new MyScalerFactoryBoxsampledJis());
        }
        
        return ret;
    }
    
    private static final int[] PRL_ARR;
    static {
        final int[] arr = new int[MAX_PRL];
        int i = 0;
        for (int prl = MIN_PRL; prl <= MAX_PRL; prl *= 2) {
            arr[i++] = prl;
        }
        PRL_ARR = Arrays.copyOfRange(arr, 0, i);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyScalerData {
        final InterfaceScalerFactoryForTest scalerFactory;
        final SortedMap<Integer,MyPrlData> prlDataByPrl =
            new TreeMap<>();
        final SortedMap<Integer,Double> crossPrlAndSpanTimeSByThreshold =
            new TreeMap<>();
        public MyScalerData(InterfaceScalerFactoryForTest scalerFactory) {
            this.scalerFactory = scalerFactory;
        }
        public MyPrlData getOrCreatePrlData(int prl) {
            MyPrlData ret = this.prlDataByPrl.get(prl);
            if (ret == null) {
                ret = new MyPrlData();
                this.prlDataByPrl.put(prl, ret);
            }
            return ret;
        }
    }
    
    private static class MyPrlData {
        final SortedMap<Integer,MyThresholdData> thrDataByThr =
            new TreeMap<>();
        public MyPrlData() {
        }
        public MyThresholdData getOrCreateThresholdData(int threshold) {
            MyThresholdData ret = this.thrDataByThr.get(threshold);
            if (ret == null) {
                ret = new MyThresholdData(threshold);
                this.thrDataByThr.put(threshold, ret);
            }
            return ret;
        }
        /**
         * @return The threshold for which mean time sum across spans
         *         is the smallest.
         */
        public int computeBestThreshold() {
            double minTimeS = Double.POSITIVE_INFINITY;
            for (MyThresholdData thresholdData : this.thrDataByThr.values()) {
                minTimeS = Math.min(minTimeS, thresholdData.crossSpanTimeS);
            }
            
            int bestThreshold = 0;
            double bestThresholdTimeS = Double.POSITIVE_INFINITY;
            for (MyThresholdData thresholdData : this.thrDataByThr.values()) {
                final int threshold = thresholdData.threshold;
                final double thresholdTimeS = thresholdData.crossSpanTimeS;
                if ((thresholdTimeS < bestThresholdTimeS)
                    || ((threshold < bestThreshold)
                        && (thresholdTimeS < minTimeS * ALLOWED_SLOWER_FACTOR))) {
                    bestThresholdTimeS = thresholdTimeS;
                    bestThreshold = threshold;
                }
            }
            return bestThreshold;
        }
    }
    
    private static class MyThresholdData {
        final int threshold;
        final SortedMap<Integer,MySpanData> spanDataByDstSpan =
            new TreeMap<>();
        double crossSpanTimeS;
        public MyThresholdData(int threshold) {
            this.threshold = threshold;
        }
        public MySpanData getOrCreateSpanData(int dstSpan) {
            MySpanData ret = this.spanDataByDstSpan.get(dstSpan);
            if (ret == null) {
                ret = new MySpanData();
                this.spanDataByDstSpan.put(dstSpan, ret);
            }
            return ret;
        }
    }
    
    private static class MySpanData {
        private double timeSumS = 0.0;
        private long timeCount = 0;
        public MySpanData() {
        }
        @Override
        public String toString() {
            return "[meanTimeS="
                + this.getMeanTimeS()
                + ",timeCount="
                + this.timeCount
                + "]";
        }
        void onNewTimeS(double newTimeS) {
            this.timeSumS += newTimeS;
            this.timeCount++;
        }
        float getMeanTimeS() {
            return (float) (this.timeSumS / this.timeCount);
        }
    }
    
    /*
     * 
     */
    
    private interface InterfaceScalerFactoryForTest {
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit);
    }
    
    private static class MyScalerFactoryNearestAwt implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryNearestAwt() {
        }
        @Override
        public String toString() {
            return "AWT-NEAR";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerNearestAwt(dstAreaThresholdForSplit);
        }
    }
    
    private static class MyScalerFactoryBilinearAwt implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryBilinearAwt() {
        }
        @Override
        public String toString() {
            return "AWT-BILI";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerBilinearAwt(dstAreaThresholdForSplit);
        }
    }
    
    private static class MyScalerFactoryBicubicAwt implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryBicubicAwt() {
        }
        @Override
        public String toString() {
            return "AWT-BICU";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerBicubicAwt(dstAreaThresholdForSplit);
        }
    }

    private static class MyScalerFactoryNearestJis implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryNearestJis() {
        }
        @Override
        public String toString() {
            return "JIS-NEAR";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerNearestJis(dstAreaThresholdForSplit);
        }
    }

    private static class MyScalerFactoryBilinearJis implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryBilinearJis() {
        }
        @Override
        public String toString() {
            return "JIS-BILI";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerBilinearJis(dstAreaThresholdForSplit);
        }
    }

    private static class MyScalerFactoryBicubicJis implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryBicubicJis() {
        }
        @Override
        public String toString() {
            return "JIS-BICU";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            return new ScalerBicubicJis(dstAreaThresholdForSplit);
        }
    }

    private static class MyScalerFactoryBoxsampledJis implements InterfaceScalerFactoryForTest {
        public MyScalerFactoryBoxsampledJis() {
        }
        @Override
        public String toString() {
            return "JIS-BOX";
        }
        @Override
        public InterfaceScaler newScaler(int dstAreaThresholdForSplit) {
            // Using it for both.
            final int srcAreaThresholdForSplit = dstAreaThresholdForSplit;
            return new ScalerBoxsampledJis(
                srcAreaThresholdForSplit,
                dstAreaThresholdForSplit);
        }
    }

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private ThresholdTuningMain() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        final long a = System.nanoTime();
        System.out.println("--- " + ThresholdTuningMain.class.getSimpleName() + "... ---");
        
        bench_thresholds_scalerFactoryList(
            newScalerFactoryList());
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + ThresholdTuningMain.class.getSimpleName()
            + ", " + JisTestUtils.toStringTimeMeasureNs(b-a) + " s ---");
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param scalerFactoryList (in)
     */
    private static void bench_thresholds_scalerFactoryList(
        List<InterfaceScalerFactoryForTest> scalerFactoryList) {
        
        final List<BufferedImageHelper> srcHelperByDstSpanIndex =
            new ArrayList<>();
        final List<BufferedImageHelper> dstHelperByDstSpanIndex =
            new ArrayList<>();
        for (int dstSpan : DST_SPAN_ARR) {
            final BufferedImageHelper srcHelper;
            {
                final int srcWidth = dstSpan * 3;
                final int srcHeight = dstSpan / 3;
                final BufferedImage srcImage =
                    newImageArgbPre(
                        srcWidth,
                        srcHeight);
                srcHelper = new BufferedImageHelper(srcImage);
                final Random random = BihTestUtils.newRandom();
                final boolean opaque = true;
                BihTestUtils.randomizeHelper(
                    random,
                    srcHelper,
                    opaque);
            }
            srcHelperByDstSpanIndex.add(srcHelper);
            
            final BufferedImageHelper dstHelper;
            {
                final BufferedImage dstImage =
                    newImageArgbPre(
                        dstSpan,
                        dstSpan);
                dstHelper = new BufferedImageHelper(dstImage);
            }
            dstHelperByDstSpanIndex.add(dstHelper);
        }

        for (InterfaceScalerFactoryForTest scalerFactory : scalerFactoryList) {
            
            System.out.println();
            
            final MyScalerData scalerData = new MyScalerData(
                scalerFactory);
            
            // Best cross span threshold for each prl.
            final List<Integer> prlCrossSpanBestThresholdByPrlIndex = new ArrayList<>();
            final Set<Integer> prlCrossSpanBestThresholdSet = new TreeSet<>();
            
            for (int prl : PRL_ARR) {
                
                for (int threshold : THRESHOLD_ARR) {
                    bench_scaler_prl_threshold(
                        srcHelperByDstSpanIndex,
                        dstHelperByDstSpanIndex,
                        scalerData,
                        prl,
                        threshold);
                }
                
                final MyPrlData prlData = scalerData.getOrCreatePrlData(prl);
                
                final int prlCrossSpanBestThreshold = prlData.computeBestThreshold();
                prlCrossSpanBestThresholdByPrlIndex.add(prlCrossSpanBestThreshold);
                prlCrossSpanBestThresholdSet.add(prlCrossSpanBestThreshold);
                
                final MyThresholdData thresholdData =
                    prlData.getOrCreateThresholdData(prlCrossSpanBestThreshold);
                System.out.println(scalerFactory
                    + ", prl="
                    + prl
                    + ", prlCrossSpanBestThr="
                    + prlCrossSpanBestThreshold
                    + ", crossSpanS="
                    + (float) thresholdData.crossSpanTimeS);
            }
            
            /*
             * Computing time, across prl and spans,
             * for each threshold.
             */
            
            int bestCrossPrlAndSpanThreshold = 0;
            double bestCrossPrlAndSpanThresholdTimeS = Double.POSITIVE_INFINITY;
            
            {
                double minTimeS = Double.POSITIVE_INFINITY;
                for (int threshold : THRESHOLD_ARR) {
                    double crossPrlAndSpanTimeS = 0.0;
                    for (MyPrlData prlData : scalerData.prlDataByPrl.values()) {
                        final MyThresholdData thresholdData =
                            prlData.thrDataByThr.get(threshold);
                        crossPrlAndSpanTimeS += thresholdData.crossSpanTimeS;
                    }
                    scalerData.crossPrlAndSpanTimeSByThreshold.put(threshold, crossPrlAndSpanTimeS);
                    
                    minTimeS = Math.min(minTimeS, crossPrlAndSpanTimeS);
                }
                
                for (int threshold : THRESHOLD_ARR) {
                    final double crossPrlAndSpanTimeS =
                        scalerData.crossPrlAndSpanTimeSByThreshold.get(threshold);
                    if ((crossPrlAndSpanTimeS < bestCrossPrlAndSpanThresholdTimeS)
                        || ((threshold < bestCrossPrlAndSpanThreshold)
                            && (crossPrlAndSpanTimeS < minTimeS * ALLOWED_SLOWER_FACTOR))) {
                        bestCrossPrlAndSpanThreshold = threshold;
                        bestCrossPrlAndSpanThresholdTimeS = crossPrlAndSpanTimeS;
                    }
                }
            }
            
            /*
             * Synthesis and best-chosing.
             */
            
            int bestPrlCrossSpanThreshold = 0;
            double bestPrlCrossSpanThresholdCrossPrlAndSpanTimeS = Double.POSITIVE_INFINITY;

            {
                double minTimeS = Double.POSITIVE_INFINITY;
                for (int prlBestThreshold : prlCrossSpanBestThresholdSet) {
                    final double crossPrlAndSpanTimeS =
                        scalerData.crossPrlAndSpanTimeSByThreshold.get(prlBestThreshold);
                    minTimeS = Math.min(minTimeS, crossPrlAndSpanTimeS);
                }
                
                for (int prlBestThreshold : prlCrossSpanBestThresholdSet) {
                    final double crossPrlAndSpanTimeS =
                        scalerData.crossPrlAndSpanTimeSByThreshold.get(prlBestThreshold);
                    System.out.println(scalerFactory
                        + ", prlBestThr="
                        + prlBestThreshold
                        + ", crossPrlAndSpanS="
                        + (float) crossPrlAndSpanTimeS);
                    if ((crossPrlAndSpanTimeS < bestPrlCrossSpanThresholdCrossPrlAndSpanTimeS)
                        || ((prlBestThreshold < bestPrlCrossSpanThreshold)
                            && (crossPrlAndSpanTimeS < minTimeS * ALLOWED_SLOWER_FACTOR))) {
                        bestPrlCrossSpanThreshold = prlBestThreshold;
                        bestPrlCrossSpanThresholdCrossPrlAndSpanTimeS = crossPrlAndSpanTimeS;
                    }
                }
            }
            
            System.out.println(scalerFactory
                + ", bestPrlCrossSpanThr="
                + bestPrlCrossSpanThreshold
                + ", crossPrlAndSpanS="
                + (float) bestPrlCrossSpanThresholdCrossPrlAndSpanTimeS);
            
            System.out.println(scalerFactory
                + ", bestCrossPrlAndSpanThr="
                + bestCrossPrlAndSpanThreshold
                + ", crossPrlAndSpanS="
                + (float) bestCrossPrlAndSpanThresholdTimeS);
        }
    }
    
    /*
     * 
     */
    
    private static void bench_scaler_prl_threshold(
        List<BufferedImageHelper> srcHelperByDstSpanIndex,
        List<BufferedImageHelper> dstHelperByDstSpanIndex,
        MyScalerData scalerData,
        int prl,
        int threshold) {
        
        final Executor parallelExecutor = JisTestUtils.newPrlExec(prl);
        
        final InterfaceScaler scaler =
            scalerData.scalerFactory.newScaler(
                threshold);
        
        final MyPrlData prlData = scalerData.getOrCreatePrlData(prl);
        final MyThresholdData thresholdData = prlData.getOrCreateThresholdData(threshold);
        
        /*
         * Benching threshold for each dst span.
         */
        
        for (int si = 0; si < DST_SPAN_ARR.length; si++) {
            final int dstSpan = DST_SPAN_ARR[si];
            final int dstArea = dstSpan * dstSpan;
            
            final MySpanData spanData =
                thresholdData.getOrCreateSpanData(dstSpan);
            
            final BufferedImageHelper srcHelper =
                srcHelperByDstSpanIndex.get(si);
            final BufferedImageHelper dstHelper =
                dstHelperByDstSpanIndex.get(si);
            
            measureTimeS(
                WARMUP_CALL_COUNT,
                scaler,
                srcHelper,
                dstHelper,
                parallelExecutor);
            
            for (int ti = 0; ti < NBR_OF_TIMINGS; ti++) {
                
                // For each measured work to do the same amount of work
                // (works if work amount independant of src area,
                // or if src area ~= dst area).
                final int nbrOfCalls = MAX_DST_AREA / dstArea;
                
                final double newTimeS =
                    measureTimeS(
                        nbrOfCalls,
                        scaler,
                        srcHelper,
                        dstHelper,
                        parallelExecutor);
                spanData.onNewTimeS(newTimeS);
            }
        }
        
        /*
         * 
         */
        
        double crossSpansTimeS = 0.0;
        double maxSpanTimeS = 0.0;
        for (MySpanData spanData : thresholdData.spanDataByDstSpan.values()) {
            final double meanTimeS = spanData.getMeanTimeS();
            crossSpansTimeS += meanTimeS;
            maxSpanTimeS = Math.max(maxSpanTimeS, meanTimeS);
        }
        thresholdData.crossSpanTimeS = crossSpansTimeS;
        if (MUST_LOG_DETAILS) {
            System.out.println(scalerData.scalerFactory
                + ", thr="
                + threshold
                + ", prl="
                + prl
                + ", crossSpanS="
                + (float) thresholdData.crossSpanTimeS
                + ", maxSpanS=" + (float) maxSpanTimeS);
        }
    }
    
    /*
     * 
     */
    
    private static double measureTimeS(
        int nbrOfCalls,
        InterfaceScaler scaler,
        //
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        Executor parallelExecutor) {
        
        final long a = System.nanoTime();
        for (int i = 0; i < nbrOfCalls; i++) {
            scaler.scaleImage(
                srcHelper,
                dstHelper,
                parallelExecutor);
            if (dstHelper.getNonPremulArgb32At(0, 0) == 0) {
                System.out.println("rare");
            }
        }
        final long b = System.nanoTime();
        final long dtNs = b - a;
        final double dtS = dtNs / 1e9;
        return dtS;
    }
    
    /*
     * 
     */
    
    /**
     * @return A TYPE_INT_ARGB_PRE image.
     */
    private static BufferedImage newImageArgbPre(int width, int height) {
        return new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB_PRE);
    }
}
