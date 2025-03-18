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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import net.jimsizr.test.tuning.types.BaseScenario;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.test.utils.TestLogger;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyInterKinds;
import net.jimsizr.types.InterKind;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Main to test copy speed and accuracy depending on
 * algo brand (AWT/JIS) and preliminary intermediary images.
 * 
 * Uses JIS treatments as reference for accuracy.
 * 
 * Only tuned TYPE_CUSTOM with types corresponding to a BihPixelFormat,
 * in which case JIS should be better than AWT.
 * For custom types for which BihPixelFormat is null, JIS should still
 * be better than AWT, or not much worse, at least for worse AWT cases,
 * for which much time is spent in "sun.java2d.loops.MaskBlit",
 * so that's still good.
 */
public class BestCopyAlgoTuningMain extends BaseBestAlgoTuning {
    
    //--------------------------------------------------------------------------
    // INHERITED CONFIGURATION
    //--------------------------------------------------------------------------
    
    /*
     * Cf. super class.
     */
    
    //--------------------------------------------------------------------------
    // COPY SPECIFIC CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean MUST_SKIP_MULTI_INTER_KIND_FOR_ALL = false;
    
    /**
     * Only useful for AWT (drawImage() being fancy).
     */
    private static final boolean MUST_USE_BINARY_SPECIFIC_SKIPS = true && MUST_USE_AWT;
    
    /**
     * First prime past 2048.
     */
    private static final int SPAN = 2053 * SPAN_FACTOR;
    
    /*
     * 
     */
    
    /**
     * We are benching copy algos,
     * so want to use the fastest copy algo,
     * as actual copy algos should.
     * 
     * Static mutable, but only used in main.
     */
    private static final ConfigurableCopyScaler copyForInterKind =
        new ConfigurableCopyScaler();
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final TestLogger LOGGER = new TestLogger(
        BestCopyAlgoTuningMain.class,
        MUST_OUTPUT_IN_FILE,
        "src/test/java");
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BestCopyAlgoTuningMain() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        final long a = System.nanoTime();
        LOGGER.println("--- " + BestCopyAlgoTuningMain.class.getSimpleName() + "... ---");
        
        if (MUST_BENCH_PRL) {
            bench_speedAndAccuracy(true);
        }
        
        if (MUST_BENCH_SEQ) {
            bench_speedAndAccuracy(false);
        }
        
        final long b = System.nanoTime();
        LOGGER.println("--- ..." + BestCopyAlgoTuningMain.class.getSimpleName()
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
            printCopyAlgoIndexConstants();
            
            /*
             * 
             */
            
            LOGGER.println();
            LOGGER.println("public static void initBestCopyAlgoIndexByCopyUseCaseIndex(byte[] a) {");
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
        
        final int spanCount = 1;
        final Map<TestImageTypeEnum,BufferedImageHelper>[] srcHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        final Map<TestImageTypeEnum,BufferedImageHelper>[] dstHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        final Map<TestImageTypeEnum,BufferedImageHelper>[] expHelperByImageTypeEnumBySpanIndex =
            new TreeMap[spanCount];
        
        /*
         * 
         */
        
        final List<BaseScenario> scenarioList = newScenarioList(prlElseSeq);
        
        TestImageTypeEnum prevSrcForLog = null;
        
        boolean didLogSinceEnteredLoggedMethod = false;
        
        for (int i = 0; i < scenarioList.size(); i++) {
            
            if (DEBUG) {
                LOGGER.println();
                LOGGER.println("test case " + (i + 1));
            }
            
            final BaseScenario scenario = scenarioList.get(i);
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
            
            final int spanIndex = 0;
            
            final int width = SPAN;
            final int height = width;
            final Map<TestImageTypeEnum,BufferedImageHelper> srcHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    srcHelperByImageTypeEnumBySpanIndex,
                    spanIndex);
            final Map<TestImageTypeEnum,BufferedImageHelper> dstHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    dstHelperByImageTypeEnumBySpanIndex,
                    spanIndex);
            final Map<TestImageTypeEnum,BufferedImageHelper> expHelperByImageTypeEnum =
                getOrCreateHelperMap(
                    expHelperByImageTypeEnumBySpanIndex,
                    spanIndex);
            
            final BufferedImageHelper srcHelper =
                getOrCreateHelper(
                    scenario.getSrcImageTypeEnum(),
                    width,
                    height,
                    srcHelperByImageTypeEnum,
                    true, // mustFill
                    false, // mustUseCloseNeightborsDeltas
                    random);
            
            final BufferedImageHelper dstHelper =
                getOrCreateHelper(
                    scenario.getDstImageTypeEnum(),
                    width,
                    height,
                    dstHelperByImageTypeEnum,
                    false, // mustFill
                    false, // mustUseCloseNeightborsDeltas
                    null);
            
            final BufferedImageHelper expHelper =
                getOrCreateHelper(
                    scenario.getDstImageTypeEnum(),
                    width,
                    height,
                    expHelperByImageTypeEnum,
                    false, // mustFill
                    false, // mustUseCloseNeightborsDeltas
                    null);
            
            /*
             * 
             */
            
            final Executor prlExecutorForScenario =
                getOrCreatePrlExec(
                    prlExecByPrl,
                    PRL_FOR_TEST,
                    scenario.isPrlElseSeq());
            
            final SortedMap<CopyAlgo,Long> minTimeNsByAlgo = new TreeMap<>();
            final SortedMap<CopyAlgo,Long> maxCptDeltaByAlgo = new TreeMap<>();
            
            final List<CopyAlgo> algoList = new ArrayList<>();
            for (CopyAlgo algo : CopyAlgo.valueList()) {
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
            }
            
            // Reference copy of input image.
            JisTestUtils.copyImage_reference(
                srcHelper,
                expHelper);

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
                for (CopyAlgo algo : algoList) {
                    if (MUST_RANDOMIZE_OUTPUT) {
                        COPY_BIH.scaleImage(
                            dstHelperRandomized,
                            dstHelper,
                            maxPrlExecutor);
                    }
                    
                    // For tiny warmup (class load).
                    measureTimeNs(
                        algo,
                        tinySrcHelper,
                        tinyDstHelper,
                        prlExecutorForScenario);

                    long minTimeNs = Long.MAX_VALUE;
                    long prevTimeNs = Long.MAX_VALUE;
                    int similarTimingCount = 0;
                    while (true) {
                        final long timeNs = measureTimeNs(
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
                        final int neighborDelta = 0;
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
    
    private static List<BaseScenario> newScenarioList(boolean prlElseSeq) {
        final List<BaseScenario> ret = new ArrayList<>();
        for (TestImageTypeEnum srcImageTypeEnum : TestImageTypeEnum.values()) {
            for (TestImageTypeEnum dstImageTypeEnum : TestImageTypeEnum.values()) {
                if (!mustUseImageTypeEnumPair(
                    srcImageTypeEnum,
                    dstImageTypeEnum)) {
                    continue;
                }
                ret.add(new BaseScenario(
                    srcImageTypeEnum,
                    dstImageTypeEnum,
                    prlElseSeq));
            }
        }
        return ret;
    }
    
    private static boolean mustUseAlgoForScenario(
        BaseScenario scenario,
        CopyAlgo algo) {
        
        if (!mustUseXxxAlgoForXxxScenario(scenario, algo)) {
            return false;
        }
        
        final CopyInterKinds interKinds = algo.getInterKinds();
        final boolean gotIK = (interKinds.interKindCount() != 0);
        final InterKind firstIK = interKinds.firstInterKind();
        final InterKind lastIK = interKinds.lastInterKind();
        
        if (MUST_SKIP_MULTI_INTER_KIND_FOR_ALL
            && (interKinds.interKindCount() >= 2)) {
            return false;
        }
        
        if (MUST_USE_BINARY_SPECIFIC_SKIPS
            && (scenario.getDstImageTypeEnum() != TestImageTypeEnum.TYPE_BYTE_BINARY)
            && (interKinds.interKindCount() >= 2)) {
            /*
             * Multi-intermediary images only useful for BINARY destination.
             */
            return false;
        }
        
        if (gotIK) {
            if (firstIK.getImageType() == scenario.getSrcImageTypeEnum().imageType()) {
                // Same firstIK as src: irrelevant.
                return false;
            }
            
            if (lastIK.getImageType() == scenario.getDstImageTypeEnum().imageType()) {
                // Same lastIK as dst: irrelevant.
                return false;
            }
            
            if (firstIK.getImageType() == scenario.getDstImageTypeEnum().imageType()) {
                // Same firstIK as dst: irrelevant.
                return false;
            }
            if (lastIK.getImageType() == scenario.getSrcImageTypeEnum().imageType()) {
                // Same lastIK as src: (normally) irrelevant.
                if (MUST_USE_BINARY_SPECIFIC_SKIPS
                    && (scenario.getDstImageTypeEnum() == TestImageTypeEnum.TYPE_BYTE_BINARY)) {
                    /*
                     * Allowing it, because for ARGB->BIN,
                     * the fastest (and accurate) is ARGB->GRAY->ARGB->BIN.
                     */
                } else {
                    return false;
                }
            }
            
            if (scenario.getSrcImageTypeEnum().hasAlpha()
                && scenario.getDstImageTypeEnum().hasAlpha()
                && interKinds.containsOpaque()) {
                // Both src and dst have alpha,
                // and an inter kind is opaque:
                // would lose alpha: irrelevant.
                return false;
            }
            
            if (scenario.getSrcImageTypeEnum().isWithColor()
                && scenario.getDstImageTypeEnum().isWithColor()
                && interKinds.containsBlackAndWhite()) {
                // Both src and dst are colored,
                // and an inter kind is black and white:
                // would lose color: irrelevant.
                return false;
            }
        }
        
        return true;
    }
    
    /*
     * 
     */
    
    private static long measureTimeNs(
        CopyAlgo algo,
        //
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        Executor parallelExecutor) {
        
        copyForInterKind.configure(algo);
        
        final long a = System.nanoTime();
        {
            copyForInterKind.scaleImage(
                srcHelper,
                dstHelper,
                parallelExecutor);
            
            helperBlackHole(dstHelper);
        }
        final long b = System.nanoTime();
        return b - a;
    }
    
    /*
     * 
     */
    
    private static void printCopyAlgoIndexConstants() {
        AlgoBrand prevBrand = null;
        InterKind prevFirstIK = null;
        for (CopyAlgo algo : CopyAlgo.valueList()) {
            final CopyInterKinds interKinds = algo.getInterKinds();
            final InterKind firstIK = interKinds.firstInterKind();
            final AlgoBrand brand = algo.getBrand();
            
            final int algoIndex =
                CopyAlgo.computeCopyAlgoIndex(
                    interKinds,
                    brand);
            
            if ((brand != prevBrand)
                || (firstIK != prevFirstIK)) {
                LOGGER.println();
                prevBrand = brand;
                prevFirstIK = firstIK;
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
