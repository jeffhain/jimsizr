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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.test.tuning.types.AlgoMeas;
import net.jimsizr.test.tuning.types.BaseScenario;
import net.jimsizr.test.tuning.types.ResizeScenario;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.test.utils.TestLogger;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyInterKinds;
import net.jimsizr.types.InterfaceAlgo;
import net.jimsizr.types.ScalingDirAndMag;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

public class BaseBestAlgoTuning {
    
    //--------------------------------------------------------------------------
    // INHERITED CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final boolean DEBUG = false;
    
    protected static final boolean MUST_LOG_CODE_ELSE_JUST_STATS = true;
    
    protected static final boolean MUST_OUTPUT_IN_FILE = false;
    
    protected static final boolean MUST_BENCH_PRL = true;
    protected static final boolean MUST_BENCH_SEQ = true;
    
    /**
     * To check that algo choices are about the same
     * with larger images.
     */
    protected static final int SPAN_FACTOR = 1;
    
    /**
     * Useful to continue code generation or bench
     * where it was interrupted.
     * 
     * -1 for invalid (i.e. start at 0).
     */
    protected static final int MUST_RESUME_AT_CASE_INDEX = -1;
    
    protected static final boolean MUST_USE_AWT = true;
    protected static final boolean MUST_USE_JIS = true;
    /**
     * AWT can be very slow with custom types,
     * so we want to allow JIS for them.
     * 
     * JIS can be faster than AWT when no type is custom,
     * but only when array direct use is allowed,
     * so to avoid much slowness when it's not,
     * we don't try JIS when neither source nor destination
     * is a custom type.
     * 
     * This also makes tunings much faster
     * (although JIS is still used for each scenario
     * to compute expected result).
     */
    protected static final boolean MUST_ONLY_ALLOW_JIS_IF_CUSTOM = true;
    protected static final boolean MUST_ONLY_USE_RAW_ALGOS = false;
    
    /*
     * 
     */
    
    /**
     * Useful for warmup, when only testing a few cases.
     */
    protected static final int RUN_COUNT = 1;
    
    /**
     * If not doing that, we don't detect issues due to AWT drawImage()
     * sometimes not properly using "Src" alpha composite.
     */
    protected static final boolean MUST_RANDOMIZE_OUTPUT = true;
    
    protected static final int MAX_PRL = Runtime.getRuntime().availableProcessors();
    
    protected static final int PRL_FOR_TEST = MAX_PRL;
    
    /*
     * 
     */
    
    protected static boolean mustUseImageTypeEnumPair(
        TestImageTypeEnum srcImageTypeEnum,
        TestImageTypeEnum dstImageTypeEnum) {
        if (false) {
            /*
             * For specific conversion.
             */
            return (srcImageTypeEnum == TestImageTypeEnum.TYPE_INT_RGB)
                && (dstImageTypeEnum == TestImageTypeEnum.TYPE_BYTE_BINARY);
        }
        if (MUST_USE_TUNING_REDUCED_IMAGE_TYPES) {
            if ((!isValidForCodeGen(srcImageTypeEnum))
                || (!isValidForCodeGen(dstImageTypeEnum))) {
                return false;
            }
        }
        return true;
    }
    
    protected static boolean isValidForCodeGen(TestImageTypeEnum imageTypeEnum) {
        /*
         * Must only use one type for CUSTOM and one for CUSTOM_PRE.
         */
        return (imageTypeEnum.imageType() != BufferedImage.TYPE_CUSTOM)
            || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR_PRE)
            || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR);
    }
    
    protected static final InterfaceScaler FAST_ACCURATE_COPY =
        new ScalerCopySmart() {
        @Override
        protected CopyAlgo getCopyAlgo(
            BufferedImageHelper srcHelper,
            BufferedImageHelper dstHelper,
            boolean prlElseSeq) {
            return CopyAlgo.valueOf(CopyInterKinds.NONE, AlgoBrand.JIS);
        }
    };
    
    /**
     * Copy with BufferedImageHelper.copyImage(),
     * possibly in parallel.
     * Only eventually inexact when copy involves binary type.
     * Useful for fast recurrent copies,
     * such as to ensure same randomized output for each test.
     */
    protected static final ConfigurableCopyScaler COPY_BIH =
        new ConfigurableCopyScaler() {
        @Override
        public void configure(CopyAlgo copyAlgo) {
            throw new UnsupportedOperationException();
        }
    };
    
    //--------------------------------------------------------------------------
    // PRIVATE CONFIGURATION
    //--------------------------------------------------------------------------
    
    /*
     * Never chosing an algo as best if it is more inaccurate.
     */

    /**
     * AWT drawImage() can have huge error for some cases
     * (colored to TYPE_BYTE_BINARY (intensity threshold),
     * TYPE_3BYTE_RGB to/from TYPE_CUSTOM, etc.),
     * but "smart" algos are designed to guard against these,
     * by using intermediary images, which allows to get
     * the max error down to 3 and sometimes 0, or by using JIS,
     * so for the general case we aim for good accuracy.
     * 
     * +1 for security, not to use very slow algo if unlucky.
     */
    private static final int ALLOWED_MAX_CPT_DELTA_OTHERS = 4;
    
    /**
     * AWT drawImage() error gets up to 9 from premul
     * to TYPE_USHORT_5X5_RGB, but is much faster so it's worth it.
     * 
     * +1 for security, not to use very slow algo if unlucky.
     */
    private static final int ALLOWED_MAX_CPT_DELTA_PREMUL_TO_US_5X5_RGB = 10;
    
    /**
     * AWT drawImage() error gets up to 81 for INDEXED destination,
     * but is much faster than JIS so we still prefer it
     * in spite of that, INDEXED not being meant to be
     * much accurate anyway.
     * 
     * +1 for security, not to use very slow algo if unlucky.
     */
    private static final int ALLOWED_MAX_CPT_DELTA_DST_IND = 82;
    
    /*
     * 
     */
    
    private static final boolean MUST_USE_TUNING_REDUCED_IMAGE_TYPES =
        true || MUST_LOG_CODE_ELSE_JUST_STATS;
    
    private static final int MAX_FASTEST_MEAS_COUNT_IN_COMMENT = 3;

    /**
     * Preferring an algo up to 10 percents slower
     * (less would make it too vulnerable to timing inaccuracies)
     * if it is a bit more accurate or uses less intermediary images.
     */
    private static final double ALLOWED_SLOWER_FACTOR = 1.1;
    
    /*
     * Taking timing into account after a number of consecutive
     * close timings.
     * Allows to avoid warmup or other spikes issues.
     * 
     * Using different (max delta, min counts) depending on timing,
     * for accurate measures of best (fasters) impls,
     * and less time wasted on worse (slowest) impls.
     */
    
    private static final int AREA_TIMING_FACTOR =
        SPAN_FACTOR * SPAN_FACTOR;
    
    private static final double FAST_TIMING_MAX_REL_DELTA = 0.025;
    private static final int FAST_TIMING_MIN_SIMILAR_COUNT = 4;
    
    private static final long FAST_TIMING_MAX_NS =
        TimeUnit.MILLISECONDS.toNanos(10L * AREA_TIMING_FACTOR);
    
    private static final double MID_TIMING_MAX_REL_DELTA = 0.05;
    private static final int MID_TIMING_MIN_SIMILAR_COUNT = 4;
    
    /**
     * All best algos are below that.
     */
    private static final long MID_TIMING_MAX_NS =
        TimeUnit.MILLISECONDS.toNanos(25L * AREA_TIMING_FACTOR);
    
    private static final double SLOW_TIMING_MAX_REL_DELTA = 0.1;
    private static final int SLOW_TIMING_MIN_SIMILAR_COUNT = 2;
    
    /**
     * Anything once above is clearly not best.
     */
    private static final long SLOW_TIMING_MAX_NS =
        TimeUnit.MILLISECONDS.toNanos(100L * AREA_TIMING_FACTOR);
    
    private static final double SLOWEST_TIMING_MAX_REL_DELTA = Double.POSITIVE_INFINITY;
    private static final int SLOWEST_TIMING_MIN_SIMILAR_COUNT = 1;
    
    /*
     * 
     */
    
    /**
     * Not too large else need more tolerance.
     * Large enough for colors to move around.
     */
    private static final int MAX_ORTHO_NEIGHBOR_DELTA = 0xF;
    
    /**
     * If small, color can be altered a lot due to resizing
     * operating in alpha-premultiplied color format.
     * We want to detect that, so not using a too large value.
     * Allows to detect cases where drawImage() just ignores alpha.
     */
    private static final int MIN_ORTHO_TRANSLUCENT_ALPHA8 = 0x80;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    protected BaseBestAlgoTuning() {
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected static void logBaseConfig(
        boolean prlElseSeq,
        TestLogger logger) {
        logger.println();
        if (MUST_LOG_CODE_ELSE_JUST_STATS) {
            logger.println("/*");
        }
        logger.println("MUST_USE_AWT = " + MUST_USE_AWT);
        logger.println("MUST_USE_JIS = " + MUST_USE_JIS);
        logger.println("MUST_ONLY_ALLOW_JIS_IF_CUSTOM = " + MUST_ONLY_ALLOW_JIS_IF_CUSTOM);
        logger.println("MUST_ONLY_USE_RAW_ALGOS = " + MUST_ONLY_USE_RAW_ALGOS);
        logger.println();
        logger.println("ALLOWED_SLOWER_FACTOR = " + ALLOWED_SLOWER_FACTOR);
        logger.println("PRL_FOR_TEST = " + PRL_FOR_TEST);
        logger.println();
        logger.println("prlElseSeq = " + prlElseSeq);
        logger.println();
        if (MUST_LOG_CODE_ELSE_JUST_STATS) {
            logger.println("*/");
        }
    }
    
    /*
     * 
     */
    
    /**
     * Checks common flags.
     */
    protected static boolean mustUseXxxAlgoForXxxScenario(
        BaseScenario scenario,
        InterfaceAlgo algo) {
        
        final AlgoBrand algoBrand = algo.getBrand();
        
        if ((!MUST_USE_AWT)
            && (algoBrand == AlgoBrand.AWT)) {
            return false;
        }
        
        if ((!MUST_USE_JIS)
            && (algoBrand == AlgoBrand.JIS)) {
            return false;
        }
        
        if (MUST_ONLY_ALLOW_JIS_IF_CUSTOM
            && (algoBrand == AlgoBrand.JIS)
            && (!scenario.getSrcImageTypeEnum().isCustom())
            && (!scenario.getDstImageTypeEnum().isCustom())) {
            return false;
        }
        
        if (MUST_ONLY_USE_RAW_ALGOS
            && (!algo.isRaw())) {
            return false;
        }
        
        return true;
    }

    /*
     * 
     */
    
    protected static Executor[] newPrlExecByPrl() {
        return new Executor[MAX_PRL + 1];
    }
    
    protected static Executor getOrCreatePrlExec(
        Executor[] prlExecByPrl,
        int parallelism,
        boolean prlElseSeq) {
        Executor ret = null;
        if (prlElseSeq
            && (parallelism >= 2)) {
            ret = prlExecByPrl[parallelism];
            if (ret == null) {
                ret = JisTestUtils.newPrlExec(parallelism);
                prlExecByPrl[parallelism] = ret;
            }
        }
        return ret;
    }
    
    protected static void shutdownPrlExecIfAny(Executor[] prlExecByPrl) {
        for (Executor prlExec : prlExecByPrl) {
            if (prlExec != null) {
                JisTestUtils.shutdownNow(prlExec);
            }
        }
    }

    /*
     * 
     */
    
    protected static double getMaxRelDelta(long timeNs) {
        final double maxRelDelta;
        if (timeNs <= FAST_TIMING_MAX_NS) {
            maxRelDelta = FAST_TIMING_MAX_REL_DELTA;
        } else if (timeNs <= MID_TIMING_MAX_NS) {
            maxRelDelta = MID_TIMING_MAX_REL_DELTA;
        } else if (timeNs <= SLOW_TIMING_MAX_NS) {
            maxRelDelta = SLOW_TIMING_MAX_REL_DELTA;
        } else {
            maxRelDelta = SLOWEST_TIMING_MAX_REL_DELTA;
        }
        return maxRelDelta;
    }
    
    protected static int getMinSimilarCount(long timeNs) {
        final int minSimilarCount;
        if (timeNs <= FAST_TIMING_MAX_NS) {
            minSimilarCount = FAST_TIMING_MIN_SIMILAR_COUNT;
        } else if (timeNs <= MID_TIMING_MAX_NS) {
            minSimilarCount = MID_TIMING_MIN_SIMILAR_COUNT;
        } else if (timeNs <= SLOW_TIMING_MAX_NS) {
            minSimilarCount = SLOW_TIMING_MIN_SIMILAR_COUNT;
        } else {
            minSimilarCount = SLOWEST_TIMING_MIN_SIMILAR_COUNT;
        }
        return minSimilarCount;
    }
    
    /*
     * 
     */
    
    protected static Map<TestImageTypeEnum,BufferedImageHelper> getOrCreateHelperMap(
        Map<TestImageTypeEnum,BufferedImageHelper>[] helperByImageTypeEnumBySpanIndex,
        int spanIndex) {
        
        Map<TestImageTypeEnum,BufferedImageHelper> ret =
            (Map<TestImageTypeEnum,BufferedImageHelper>) helperByImageTypeEnumBySpanIndex[spanIndex];
        if (ret == null) {
            ret = new TreeMap<>();
            helperByImageTypeEnumBySpanIndex[spanIndex] = ret;
        }
        return ret;
    }
    
    protected static BufferedImageHelper getOrCreateHelper(
        TestImageTypeEnum imageTypeEnum,
        int width,
        int height,
        Map<TestImageTypeEnum,BufferedImageHelper> helperByImageTypeEnum,
        boolean mustFill,
        boolean mustUseCloseNeightborsDeltas,
        Random random) {
        
        // If has alpha, never opaque, to properly check
        // worse-case scenario.
        final boolean opaque = !imageTypeEnum.hasAlpha();
        
        BufferedImageHelper helper = helperByImageTypeEnum.get(imageTypeEnum);
        if (helper == null) {
            final BufferedImage srcImage =
                BihTestUtils.newImage(
                    width,
                    height,
                    imageTypeEnum);
            // Benching with array direct use allowed.
            helper = new BufferedImageHelper(srcImage);
            if (mustFill) {
                if (mustUseCloseNeightborsDeltas) {
                    JisTestUtils.randomizeCloseNeighboors(
                        random,
                        helper.getImage(),
                        MAX_ORTHO_NEIGHBOR_DELTA,
                        opaque,
                        MIN_ORTHO_TRANSLUCENT_ALPHA8);
                } else {
                    BihTestUtils.randomizeHelper(
                        random,
                        helper,
                        opaque);
                }
            }
            helperByImageTypeEnum.put(imageTypeEnum, helper);
        }
        return helper;
    }
    
    /**
     * Anti-optim.
     */
    protected static void helperBlackHole(BufferedImageHelper helper) {
        if (helper.getNonPremulArgb32At(0, 0) == 0xC4F3B4B3) {
            System.out.println("rare");
        }
    }
    
    /**
     * @return The best one, i.e. typically the fastest one,
     *         but can also return a slightly slower one,
     *         if it is a bit more accurate or uses less intermediary images,
     *         or a much slower one if fast ones are too inaccurate.
     */
    protected static AlgoMeas computeBestMeasurement(
        ScalingDirAndMag resizeDirAndMag,
        int allowedMaxCptDelta,
        List<AlgoMeas> timeSortedMeasList) {
        
        final List<AlgoMeas> accurateEnoughMeasList = new ArrayList<>();
        for (AlgoMeas meas : timeSortedMeasList) {
            if (meas.getMaxCptDelta() <= allowedMaxCptDelta) {
                accurateEnoughMeasList.add(meas);
            }
        }
        if (accurateEnoughMeasList.isEmpty()) {
            System.out.println("timeSortedMeasList = " + timeSortedMeasList);
            throw new RuntimeException("no accurate enough measure");
        }
        
        final long minMeasTimeNs = accurateEnoughMeasList.get(0).getTimeNs();
        final long allowedMaxMeasTimeNs = Math.max(
            minMeasTimeNs,
            (long) (minMeasTimeNs * ALLOWED_SLOWER_FACTOR));
        
        final List<AlgoMeas> bestSortedMeasList = new ArrayList<>();
        for (AlgoMeas meas : accurateEnoughMeasList) {
            if (meas.getTimeNs() <= allowedMaxMeasTimeNs) {
                bestSortedMeasList.add(meas);
            }
        }
        
        Collections.sort(
            bestSortedMeasList,
            new BestMeasComparator(resizeDirAndMag));
        
        return bestSortedMeasList.get(0);
    }
    
    /**
     * @param timeSortedMeasList Must be sorted by increasing time.
     */
    protected static String toStringMeasuresAsCode(
        BaseScenario scenario,
        int allowedMaxCptDelta,
        List<AlgoMeas> timeSortedMeasList) {
        final ScalingDirAndMag resizeDirAndMag;
        if (scenario instanceof ResizeScenario) {
            resizeDirAndMag = ((ResizeScenario) scenario).getDirAndMag();
        } else {
            resizeDirAndMag = null;
        }
        // Not necessarily the fastest.
        final AlgoMeas bestMeas =
            computeBestMeasurement(
                resizeDirAndMag,
                allowedMaxCptDelta,
                timeSortedMeasList);
        final StringBuilder sb = new StringBuilder();
        final int caseIndex = scenario.computeUseCaseIndex();
        sb.append("a[");
        sb.append(caseIndex);
        sb.append("]");
        if (true) {
            sb.append(" /* ");
            sb.append(scenario.toStringForCodeGen());
            sb.append(" */");
        }
        sb.append(" = ");
        sb.append(bestMeas.getAlgo().toString());
        sb.append(";");
        if (MAX_FASTEST_MEAS_COUNT_IN_COMMENT > 0) {
            boolean didLogBestMeas = false;
            boolean didLogAwtFlavor = false;
            boolean didLogJisFlavor = false;
            sb.append(" // ");
            for (int i = 0; i < timeSortedMeasList.size(); i++) {
                final AlgoMeas meas = timeSortedMeasList.get(i);
                final InterfaceAlgo measAlgo = meas.getAlgo();
                final AlgoBrand algoBrand = measAlgo.getBrand();
                final boolean isAwtFlavor = (algoBrand == AlgoBrand.AWT);
                final boolean isJisFlavor = (algoBrand == AlgoBrand.JIS);
                
                boolean mustLogMes = false;
                if (i < MAX_FASTEST_MEAS_COUNT_IN_COMMENT) {
                    mustLogMes = true;
                    if (isAwtFlavor) {
                        didLogAwtFlavor = true;
                    }
                    if (isJisFlavor) {
                        didLogJisFlavor = true;
                    }
                } else {
                    if (isAwtFlavor) {
                        if (measAlgo.isRaw()
                            || (!didLogAwtFlavor)) {
                            mustLogMes = true;
                            didLogAwtFlavor = true;
                        }
                    }
                    if (isJisFlavor) {
                        if (measAlgo.isRaw()
                            || (!didLogJisFlavor)) {
                            mustLogMes = true;
                            didLogJisFlavor = true;
                        }
                    }
                }
                
                if (meas == bestMeas) {
                    if (!didLogBestMeas) {
                        mustLogMes = true;
                        didLogBestMeas = true;
                    }
                }
                
                if (mustLogMes) {
                    if (i != 0) {
                        if (i >= MAX_FASTEST_MEAS_COUNT_IN_COMMENT) {
                            sb.append(", (...) ");
                        } else {
                            sb.append(", ");
                        }
                    }
                    sb.append(meas);
                }
            }
        }
        return sb.toString();
    }

    /**
     * @param minTimeNsByAlgo Must not be null.
     * @param maxCptDeltaByAlgo Can be null.
     */
    protected static <A extends InterfaceAlgo> void logMeasures(
        BaseScenario scenario,
        SortedMap<A,Long> minTimeNsByAlgo,
        SortedMap<A,Long> maxCptDeltaByAlgo,
        //
        TestLogger logger,
        boolean mustLogElseJustStats) {
        
        final TestImageTypeEnum srcType = scenario.getSrcImageTypeEnum();
        final TestImageTypeEnum dstType = scenario.getDstImageTypeEnum();
        
        final int allowedMaxCptDelta;
        if (dstType.isIndexed()) {
            allowedMaxCptDelta = ALLOWED_MAX_CPT_DELTA_DST_IND;
        } else if (srcType.isPremul() || dstType.isUShortRgb()) {
            allowedMaxCptDelta = ALLOWED_MAX_CPT_DELTA_PREMUL_TO_US_5X5_RGB;
        } else {
            allowedMaxCptDelta = ALLOWED_MAX_CPT_DELTA_OTHERS;
        }
        
        final List<AlgoMeas> measList = new ArrayList<>();
        for (A algo : minTimeNsByAlgo.keySet()) {
            final long minTimeNs = minTimeNsByAlgo.get(algo);
            final int maxCptDelta = (int) (long) maxCptDeltaByAlgo.get(algo);
            
            final AlgoMeas meas =
                new AlgoMeas(
                    minTimeNs,
                    maxCptDelta,
                    algo);
            measList.add(meas);
        }
        // Sorts by increasing time.
        Collections.sort(measList);
        
        if (mustLogElseJustStats) {
            logger.println(toStringMeasuresAsCode(
                scenario,
                allowedMaxCptDelta,
                measList));
        } else {
            logger.println(scenario + " = " + measList);
        }
    }
}
