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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.scalers.basic.awt.ScalerBicubicAwt;
import net.jimsizr.scalers.basic.awt.ScalerBilinearAwt;
import net.jimsizr.scalers.basic.awt.ScalerNearestAwt;
import net.jimsizr.scalers.basic.jis.ScalerBicubicJis;
import net.jimsizr.scalers.basic.jis.ScalerBilinearJis;
import net.jimsizr.scalers.basic.jis.ScalerNearestJis;
import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.test.tuning.types.ResizeScenario;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.test.utils.TestLogger;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.InterKind;
import net.jimsizr.types.ResizeAlgo;
import net.jimsizr.types.ResizeAlgoType;
import net.jimsizr.types.ScalingDirAndMag;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Main to test speed and accuracy depending on
 * algo brand (AWT/JIS) and intermediate images,
 * for copy and scaling treatments.
 * 
 * Uses JIS treatments as reference for accuracy.
 */
public class BestResizeAlgoTuningMain extends BaseBestAlgoTuning {
    
    //--------------------------------------------------------------------------
    // INHERITED CONFIGURATION
    //--------------------------------------------------------------------------
    
    /*
     * Cf. super class.
     */
    
    //--------------------------------------------------------------------------
    // RESIZE SPECIFIC CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Prime numbers just above power of two thresholds
     * that might be used for parallelization.
     */
    private static final int[] BASE_SPAN_ARR = new int[] {
        67,
        //
        131,
        //
        257,
        271, // 257+15
        //
        521,
        551, // 521 + 30 (such as 551/521 ~= 271/257)
        //
        1031,
        //
        2053};
    private static final int BASE_SPAN_INDEX_67 = 0;
    private static final int BASE_SPAN_INDEX_131 = 1;
    private static final int BASE_SPAN_INDEX_257 = 2;
    private static final int BASE_SPAN_INDEX_271 = 3;
    private static final int BASE_SPAN_INDEX_521 = 4;
    private static final int BASE_SPAN_INDEX_551 = 5;
    private static final int BASE_SPAN_INDEX_1031 = 6;
    private static final int BASE_SPAN_INDEX_2053 = 7;
    
    /*
     * 
     */
    
    /**
     * To make sure close pixels are not too different from each other,
     * to avoid huge components deltas in case of slight 2D edge cases
     * (such as for NEAREST).
     */
    private static final boolean MUST_USE_CLOSE_NEIGHBORS_DELTAS = true;
    
    /**
     * Useful even if MUST_USE_CLOSE_NEIGHBORS_DELTAS is true,
     * due to pixel choices edge cases especially when using NEAREST,
     * especially with low accuracy formats such as TYPE_BYTE_BINARY.
     */
    private static final boolean MUST_CHECK_EXP_NEIGHBORS_FOR_NEAREST = true;
    
    /*
     * 
     */
    
    /**
     * We are benching overall (intermediary-included) resize algos,
     * so want to use the fastest copy algo,
     * as actual resize algos should.
     */
    private static final ScalerCopySmart COPY_FOR_INTER_KIND = new ScalerCopySmart();
    
    /*
     * 
     */
    
    private static final Map<ResizeAlgoType,InterfaceScaler[]> SCALERS_BY_ALGO_TYPE;
    static {
        final SortedMap<ResizeAlgoType,InterfaceScaler[]> map = new TreeMap<>();
        // AWT first.
        map.put(ResizeAlgoType.NEAREST, new InterfaceScaler[]{
            new ScalerNearestAwt(),
            new ScalerNearestJis()});
        map.put(ResizeAlgoType.BILINEAR, new InterfaceScaler[]{
            new ScalerBilinearAwt(),
            new ScalerBilinearJis()});
        map.put(ResizeAlgoType.BICUBIC, new InterfaceScaler[]{
            new ScalerBicubicAwt(),
            new ScalerBicubicJis()});
        SCALERS_BY_ALGO_TYPE = Collections.unmodifiableSortedMap(map);
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final TestLogger LOGGER = new TestLogger(
        BestResizeAlgoTuningMain.class,
        MUST_OUTPUT_IN_FILE,
        "src/test/java");
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BestResizeAlgoTuningMain() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        final long a = System.nanoTime();
        LOGGER.println("--- " + BestResizeAlgoTuningMain.class.getSimpleName() + "... ---");
        
        if (MUST_BENCH_PRL) {
            bench_speedAndAccuracy(true);
        }
        
        if (MUST_BENCH_SEQ) {
            bench_speedAndAccuracy(false);
        }
        
        final long b = System.nanoTime();
        LOGGER.println("--- ..." + BestResizeAlgoTuningMain.class.getSimpleName()
            + ", " + JisTestUtils.toStringTimeMeasureNs(b-a) + " s ---");
        
        try {
            LOGGER.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void bench_speedAndAccuracy(boolean prlElseSeq) {
        
        logBaseConfig(prlElseSeq, LOGGER);
        
        if (MUST_LOG_CODE_ELSE_JUST_STATS) {
            printResizeAlgoIndexConstants();
            
            /*
             * 
             */
            
            LOGGER.println();
            LOGGER.println("public static void initBestResizeAlgoIndexByResizeUseCaseIndex(byte[] a) {");
            for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
                if (isValidForCodeGen(imageTypeEnum)) {
                    LOGGER.println(
                        "populateArrForSrc_"
                            + imageTypeEnum.toStringShort()
                            + "(a);");
                }
            }
            LOGGER.println("}");
        }
        
        /*
         * 
         */
        
        final Random random = BihTestUtils.newRandom();
        
        /*
         * parallel executors
         */
        
        final Executor[] prlExecByPrl =
            newPrlExecByPrl();
        
        final Executor maxPrlExecutor =
            getOrCreatePrlExec(
                prlExecByPrl,
                MAX_PRL,
                true);
        
        /*
         * Image caches.
         */
        
        final int spanCount = BASE_SPAN_ARR.length;
        final Map<TestImageTypeEnum,BufferedImageHelper>[] srcHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        final Map<TestImageTypeEnum,BufferedImageHelper>[] dstHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        final Map<TestImageTypeEnum,BufferedImageHelper>[] expHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        
        /*
         * 
         */
        
        final List<ResizeScenario> scenarioList = newScenarioList(prlElseSeq);
        
        TestImageTypeEnum prevSrcForLog = null;
        TestImageTypeEnum prevDstForLog = null;
        ResizeAlgoType prevAlgoTypeForLog = null;
        
        boolean didLogSinceEnteredLoggedMethod = false;
        
        for (int i = 0; i < scenarioList.size(); i++) {
            
            if (DEBUG) {
                LOGGER.println();
                LOGGER.println("test case " + (i + 1));
            }
            
            final ResizeScenario scenario = scenarioList.get(i);
            if (DEBUG) {
                LOGGER.println("scenario = " + scenario);
            }
            if (MUST_RESUME_AT_CASE_INDEX != -1) {
                final int caseIndex = scenario.computeUseCaseIndex();
                if (caseIndex < MUST_RESUME_AT_CASE_INDEX) {
                    continue;
                }
            }
            
            /*
             * Using 1031 as destination span for all resizes,
             * to be able to compare relative speeds,
             * and because it's large enough for good parallelization
             * with typical resires split thresholds.
             * 
             * Using 4099 as span for copy, which is much faster
             * than resizes and has a much larger split threshold.
             */
            
            /*
             * Areas large enough to enable some parallelism:
             * dstArea >= 256 * 256 = 64 * 1024 = 8 * (8 * 1024)
             * so can get effective parallelism of 8
             * with area split thresholds of 16 * 1024.
             */
            final int srcSpanIndex;
            final int dstSpanIndex;
            if (scenario.getDirAndMag() == ScalingDirAndMag.D8) {
                srcSpanIndex = BASE_SPAN_INDEX_2053;
                dstSpanIndex = BASE_SPAN_INDEX_257;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.D4) {
                srcSpanIndex = BASE_SPAN_INDEX_1031;
                dstSpanIndex = BASE_SPAN_INDEX_257;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.D2) {
                srcSpanIndex = BASE_SPAN_INDEX_521;
                dstSpanIndex = BASE_SPAN_INDEX_257;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.D1) {
                srcSpanIndex = BASE_SPAN_INDEX_271;
                dstSpanIndex = BASE_SPAN_INDEX_257;
                /*
                 * 
                 */
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.U1) {
                srcSpanIndex = BASE_SPAN_INDEX_521;
                dstSpanIndex = BASE_SPAN_INDEX_551;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.U2) {
                srcSpanIndex = BASE_SPAN_INDEX_257;
                dstSpanIndex = BASE_SPAN_INDEX_551;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.U4) {
                srcSpanIndex = BASE_SPAN_INDEX_131;
                dstSpanIndex = BASE_SPAN_INDEX_551;
            } else if (scenario.getDirAndMag() == ScalingDirAndMag.U8) {
                srcSpanIndex = BASE_SPAN_INDEX_67;
                dstSpanIndex = BASE_SPAN_INDEX_551;
            } else {
                throw new AssertionError();
            }
            
            final int srcWidth = BASE_SPAN_ARR[srcSpanIndex] * SPAN_FACTOR;
            final int dstWidth = BASE_SPAN_ARR[dstSpanIndex] * SPAN_FACTOR;
            final int srcHeight = srcWidth;
            final int dstHeight = dstWidth;
            final Map<TestImageTypeEnum,BufferedImageHelper> srcHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    srcHelperByImageTypeEnumBySpanIndex,
                    srcSpanIndex);
            final Map<TestImageTypeEnum,BufferedImageHelper> dstHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    dstHelperByImageTypeEnumBySpanIndex,
                    dstSpanIndex);
            final Map<TestImageTypeEnum,BufferedImageHelper> expHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    expHelperByImageTypeEnumBySpanIndex,
                    dstSpanIndex);
            
            final BufferedImageHelper srcHelper =
                getOrCreateHelper(
                    scenario.getSrcImageTypeEnum(),
                    srcWidth,
                    srcHeight,
                    srcHelperByImageTypeEnum,
                    true, // mustFill
                    MUST_USE_CLOSE_NEIGHBORS_DELTAS,
                    random);
            
            final BufferedImageHelper dstHelper =
                getOrCreateHelper(
                    scenario.getDstImageTypeEnum(),
                    dstWidth,
                    dstHeight,
                    dstHelperByImageTypeEnum,
                    false, // mustFill
                    MUST_USE_CLOSE_NEIGHBORS_DELTAS,
                    null);
            
            final BufferedImageHelper expHelper =
                getOrCreateHelper(
                    scenario.getDstImageTypeEnum(),
                    dstWidth,
                    dstHeight,
                    expHelperByImageTypeEnum,
                    false, // mustFill
                    MUST_USE_CLOSE_NEIGHBORS_DELTAS,
                    null);
            
            final InterfaceScaler[] scalerArr =
                SCALERS_BY_ALGO_TYPE.get(
                    scenario.getAlgoType());
            final InterfaceScaler scalerAwt = scalerArr[0];
            final InterfaceScaler scalerJis = scalerArr[1];
            
            /*
             * 
             */
            
            final Executor prlExecutorForScenario =
                getOrCreatePrlExec(
                    prlExecByPrl,
                    PRL_FOR_TEST,
                    scenario.isPrlElseSeq());
            
            final SortedMap<ResizeAlgo,Long> minTimeNsByAlgo = new TreeMap<>();
            final SortedMap<ResizeAlgo,Long> maxCptDeltaByAlgo = new TreeMap<>();
            
            final List<ResizeAlgo> algoList = new ArrayList<>();
            for (ResizeAlgo algo : ResizeAlgo.valueList()) {
                if (mustUseAlgoForScenario(scenario, algo)) {
                    algoList.add(algo);
                }
            }
            
            if (algoList.size() != 0) {
                if ((prevSrcForLog == null)
                    || (scenario.getSrcImageTypeEnum() != prevSrcForLog)) {
                    if (MUST_LOG_CODE_ELSE_JUST_STATS) {
                        // We always log something in a method.
                        if (didLogSinceEnteredLoggedMethod) {
                            LOGGER.println("}");
                        }
                    }
                    LOGGER.println();
                    if (MUST_LOG_CODE_ELSE_JUST_STATS) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("private static void populateArrForSrc_");
                        sb.append(scenario.getSrcImageTypeEnum().toStringShort());
                        sb.append("(byte[] a) {");
                        LOGGER.println(sb.toString());
                        didLogSinceEnteredLoggedMethod = false;
                    }
                }
                prevSrcForLog = scenario.getSrcImageTypeEnum();
                
                if ((prevDstForLog != null)
                    && (scenario.getDstImageTypeEnum() != prevDstForLog)) {
                    if (didLogSinceEnteredLoggedMethod) {
                        LOGGER.println();
                    }
                }
                prevDstForLog = scenario.getDstImageTypeEnum();
                
                if ((prevAlgoTypeForLog != null)
                    && (scenario.getAlgoType() != prevAlgoTypeForLog)) {
                    if (didLogSinceEnteredLoggedMethod) {
                        LOGGER.println();
                    }
                }
                prevAlgoTypeForLog = scenario.getAlgoType();
            }
            
            // JIS resizing as reference.
            scalerJis.scaleImage(
                srcHelper,
                expHelper,
                maxPrlExecutor);
            
            // Using the same randomized output image
            // for each algo, to be fair.
            // We copy is when needed, it's faster and simpler
            // than re-randomize it with same seed every time.
            final BufferedImageHelper dstHelperRandomized =
                BihTestUtils.newSameTypeImageAndHelper(dstHelper);
            BihTestUtils.randomizeHelper(
                random,
                dstHelperRandomized,
                false);
            
            final BufferedImageHelper tinySrcHelper =
                JisTestUtils.newSameTypeTinyImageAndHelper(
                    srcHelper);
            final BufferedImageHelper tinyDstHelper =
                JisTestUtils.newSameTypeTinyImageAndHelper(
                    dstHelper);
            
            for (int k = 0; k < RUN_COUNT; k++) {
                for (ResizeAlgo algo : algoList) {
                    if (MUST_RANDOMIZE_OUTPUT) {
                        COPY_BIH.scaleImage(
                            dstHelperRandomized,
                            dstHelper,
                            maxPrlExecutor);
                    }
                    
                    // For tiny warmup (class load).
                    measureTimeNs(
                        scalerAwt,
                        scalerJis,
                        algo,
                        tinySrcHelper,
                        tinyDstHelper,
                        prlExecutorForScenario);
                    
                    long minTimeNs = Long.MAX_VALUE;
                    long prevTimeNs = Long.MAX_VALUE;
                    int similarTimingCount = 0;
                    while (true) {
                        final long timeNs = measureTimeNs(
                            scalerAwt,
                            scalerJis,
                            algo,
                            srcHelper,
                            dstHelper,
                            prlExecutorForScenario);
                        
                        minTimeNs = Math.min(minTimeNs, timeNs);
                        
                        // +Infinity or NaN if zero time(s), so causes counter reset.
                        final double relDelta =
                            Math.abs(timeNs - (double) prevTimeNs)
                            / Math.max(timeNs, prevTimeNs);
                        prevTimeNs = timeNs;
                        
                        // Does not hurt if slow/fast changes
                        // between timings.
                        final double maxRelDelta =
                            getMaxRelDelta(timeNs);
                        final int minSimilarCount =
                            getMinSimilarCount(timeNs);
                        if (relDelta <= maxRelDelta) {
                            similarTimingCount++;
                            if (similarTimingCount >= minSimilarCount) {
                                break;
                            }
                        } else {
                            similarTimingCount = 0;
                        }
                    }
                    if (DEBUG) {
                        LOGGER.println("algo = " + algo);
                        LOGGER.println("minTimeNs = " + minTimeNs);
                    }
                    
                    {
                        final int neighborDelta =
                            getNeighborDelta(scenario.getAlgoType());
                        final int maxCptDelta =
                            BihTestUtils.computeMaxCptDelta(
                                expHelper,
                                dstHelper,
                                neighborDelta);
                        if (DEBUG) {
                            LOGGER.println("  maxCptDelta = " + maxCptDelta);
                        }
                        maxCptDeltaByAlgo.put(algo, (long) maxCptDelta);
                    }
                    
                    minTimeNsByAlgo.put(algo, minTimeNs);
                }
                
                /*
                 * 
                 */
                
                if (algoList.size() != 0) {
                    logMeasures(
                        scenario,
                        minTimeNsByAlgo,
                        maxCptDeltaByAlgo,
                        LOGGER,
                        MUST_LOG_CODE_ELSE_JUST_STATS);
                    didLogSinceEnteredLoggedMethod = true;
                }
            }
        }
        
        if (MUST_LOG_CODE_ELSE_JUST_STATS) {
            // Last method closing.
            LOGGER.println("}");
        }
        
        /*
         * Threads are daemons, so shutting down just in case
         * VM keeps running after test. 
         */
        shutdownPrlExecIfAny(prlExecByPrl);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int getNeighborDelta(ResizeAlgoType algoType) {
        final int ret;
        if (MUST_CHECK_EXP_NEIGHBORS_FOR_NEAREST
            && (algoType == ResizeAlgoType.NEAREST)) {
            /*
             * 1 should always be enough, since only issue on edge cases
             * and only one resize pass.
             * But for some reason it's not enough for downscalings
             * superior to two, possibly due to AWT "nearest" formula
             * peculiarities (doesn't give the same result as JIS).
             */
            ret = 1;
        } else {
            ret = 0;
        }
        return ret;
    }
    
    private static List<ResizeScenario> newScenarioList(boolean prlElseSeq) {
        final List<ResizeScenario> ret = new ArrayList<>();
        for (TestImageTypeEnum srcImageTypeEnum : TestImageTypeEnum.values()) {
            for (TestImageTypeEnum dstImageTypeEnum : TestImageTypeEnum.values()) {
                if (!mustUseImageTypeEnumPair(
                    srcImageTypeEnum,
                    dstImageTypeEnum)) {
                    continue;
                }
                for (ResizeAlgoType algoType : ResizeAlgoType.values()) {
                    for (ScalingDirAndMag opType : ScalingDirAndMag.values()) {
                        ret.add(new ResizeScenario(
                            srcImageTypeEnum,
                            dstImageTypeEnum,
                            prlElseSeq,
                            algoType,
                            opType));
                    }
                }
            }
        }
        return ret;
    }
    
    private static boolean mustUseAlgoForScenario(
        ResizeScenario scenario,
        ResizeAlgo algo) {
        
        if (!mustUseXxxAlgoForXxxScenario(scenario, algo)) {
            return false;
        }
        
        final InterKind srcIK = algo.getSrcIK();
        final InterKind dstIK = algo.getDstIK();
        
        if (srcIK.getImageType() == scenario.getSrcImageTypeEnum().imageType()) {
            // Same srcIK as src: irrelevant.
            return false;
        }
        
        if (dstIK.getImageType() == scenario.getDstImageTypeEnum().imageType()) {
            // Same dstIK as dst: irrelevant.
            return false;
        }
        
        if (scenario.getSrcImageTypeEnum().hasAlpha()
            && scenario.getDstImageTypeEnum().hasAlpha()
            && (srcIK.isOpaque()
                || dstIK.isOpaque())) {
            // Both src and dst have alpha,
            // and an inter kind is opaque:
            // would lose alpha: irrelevant.
            return false;
        }
        
        if (scenario.getSrcImageTypeEnum().isWithColor()
            && scenario.getDstImageTypeEnum().isWithColor()
            && (srcIK.isBlackAndWhite()
                || dstIK.isBlackAndWhite())) {
            // Both src and dst are colored,
            // and an inter kind is black and white:
            // would lose color: irrelevant.
            return false;
        }
        
        return true;
    }
    
    /*
     * 
     */
    
    private static long measureTimeNs(
        InterfaceScaler scalerAwt,
        InterfaceScaler scalerJis,
        ResizeAlgo algo,
        //
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        Executor parallelExecutor) {
        
        final InterKind srcIK = algo.getSrcIK();
        final AlgoBrand algoBrand = algo.getBrand();
        final InterKind dstIK = algo.getDstIK();
        
        final long a = System.nanoTime();
        {
            final InterfaceScaler scaler;
            if (algoBrand == AlgoBrand.AWT) {
                scaler = scalerAwt;
            } else {
                scaler = scalerJis;
            }
            
            final BufferedImageHelper srcHelperForRaw;
            if (srcIK != InterKind.NONE) {
                /*
                 * We want to do it every time:
                 * it's part of the work to time.
                 */
                srcHelperForRaw =
                    new BufferedImageHelper(
                        new BufferedImage(
                            srcHelper.getWidth(),
                            srcHelper.getHeight(),
                            srcIK.getImageType()));
                copyInterKind(
                    srcHelper,
                    srcHelperForRaw,
                    parallelExecutor);
            } else {
                srcHelperForRaw = srcHelper;
            }
            
            final BufferedImageHelper dstHelperForRaw;
            if (dstIK != InterKind.NONE) {
                dstHelperForRaw =
                    new BufferedImageHelper(
                        new BufferedImage(
                            dstHelper.getWidth(),
                            dstHelper.getHeight(),
                            dstIK.getImageType()));
            } else {
                dstHelperForRaw = dstHelper;
            }
            
            scaler.scaleImage(
                srcHelperForRaw,
                dstHelperForRaw,
                parallelExecutor);
            
            if (dstHelperForRaw != dstHelper) {
                copyInterKind(
                    dstHelperForRaw,
                    dstHelper,
                    parallelExecutor);
            }
            
            helperBlackHole(dstHelper);
        }
        final long b = System.nanoTime();
        return b - a;
    }
    
    private static void copyInterKind(
        BufferedImageHelper copyFrom,
        BufferedImageHelper copyTo,
        Executor parallelExecutor) {
        
        COPY_FOR_INTER_KIND.scaleImage(
            copyFrom,
            copyTo,
            parallelExecutor);
    }
    
    /*
     * 
     */
    
    private static void printResizeAlgoIndexConstants() {
        AlgoBrand prevBrand = null;
        for (ResizeAlgo algo : ResizeAlgo.valueList()) {
            final int algoIndex =
                ResizeAlgo.computeResizeAlgoIndex(
                    algo.getSrcIK(),
                    algo.getBrand(),
                    algo.getDstIK());
            if (algo.getBrand() != prevBrand) {
                LOGGER.println();
                prevBrand = algo.getBrand();
            }
            LOGGER.println(
                "private static final byte "
                    + algo
                    + " = "
                    + algoIndex
                    + ";");
        }
    }
}
