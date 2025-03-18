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
package net.jimsizr;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

public class JimsizrPerf {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 3;
    
    /**
     * Not too large else causes too large images in near-thresholds benches,
     * and memory becomes bottleneck for too large parallelism.
     */
    private static final int PRL =
        Math.min(16, Runtime.getRuntime().availableProcessors());
    private static final Executor PRL_EXECUTOR = JisTestUtils.newPrlExec(PRL);
    
    /*
     * 
     */
    
    private static final boolean MUST_BENCH_DOWNSCALING_WITH_NEAREST = true;
    private static final boolean MUST_BENCH_DOWNSCALING_WITH_BOXSAMPLED = true;
    private static final boolean MUST_BENCH_DOWNSCALING_WITH_BILINEAR = true;
    private static final boolean MUST_BENCH_DOWNSCALING_WITH_BICUBIC = true;
    
    /**
     * Not much different than opaque.
     */
    private static final boolean MUST_BENCH_TRANSLUCENT = false;
    
    /**
     * Else takes age, unless disabling a lot of cases.
     */
    private static final boolean MUST_ONLY_BENCH_ARGB_PRE = true;
    
    private static final boolean MUST_BENCH_UPSCALING = true;
    private static final boolean MUST_BENCH_NOSCALING = true;
    private static final boolean MUST_BENCH_DOWNSCALING = true;
    
    private static final boolean MUST_BENCH_SEQ = true;
    private static final boolean MUST_BENCH_PRL = true;
    
    /*
     * Call counts and spans,
     * each from slowest to fastest.
     */
    
    private static final int CALLS_FACTOR = 10;
    private static final int SPAN_FACTOR = 1;
    
    /**
     * 1601+1 to avoid special case for exact cases.
     */
    private static final int[][] CALLS_SRCSPAN_DSTSPAN_ARR_UPSCALING = new int[][] {
        {1, 1600, 1601},
        {1, 800, 1601},
        {1, 400, 1601},
        {1, 200, 1601},
        {1, 100, 1601},
    };
    
    private static final int[][] CALLS_SRCSPAN_DSTSPAN_ARR_NOSCALING = new int[][] {
        {256, 100, 100},
        {64, 200, 200},
        {16, 400, 400},
        {4, 800, 800},
        {1, 1601, 1601},
    };
    
    private static final int[][] CALLS_SRCSPAN_DSTSPAN_ARR_DOWNSCALING = new int[][] {
        {1, 1601, 1600},
        {1, 1601, 800},
        {1, 1601, 400},
        {1, 1601, 200},
        {1, 1601, 100},
    };
    
    /*
     * 
     */
    
    private static final List<TestImageTypeEnum> BENCHED_SRC_IMAGE_TYPE_ENUM_LIST;
    static {
        final List<TestImageTypeEnum> list = new ArrayList<>();
        
        if (MUST_ONLY_BENCH_ARGB_PRE) {
            list.add(TestImageTypeEnum.TYPE_INT_ARGB_PRE);
        } else {
            list.add(TestImageTypeEnum.TYPE_INT_RGB);
            list.add(TestImageTypeEnum.TYPE_INT_ARGB);
            list.add(TestImageTypeEnum.TYPE_INT_ARGB_PRE);
            list.add(TestImageTypeEnum.TYPE_4BYTE_ABGR_PRE);
            list.add(TestImageTypeEnum.TYPE_3BYTE_BGR);
            list.add(TestImageTypeEnum.TYPE_USHORT_565_RGB);
            list.add(TestImageTypeEnum.TYPE_USHORT_GRAY);
            list.add(TestImageTypeEnum.TYPE_BYTE_GRAY);
            list.add(TestImageTypeEnum.TYPE_BYTE_BINARY);
            list.add(TestImageTypeEnum.TYPE_BYTE_INDEXED);
        }
        
        BENCHED_SRC_IMAGE_TYPE_ENUM_LIST = Collections.unmodifiableList(list);
    }
    
    private static final List<TestImageTypeEnum> BENCHED_DST_IMAGE_TYPE_ENUM_LIST;
    static {
        final List<TestImageTypeEnum> list = new ArrayList<>();
        
        list.addAll(BENCHED_SRC_IMAGE_TYPE_ENUM_LIST);
        
        BENCHED_DST_IMAGE_TYPE_ENUM_LIST = Collections.unmodifiableList(list);
    }
    
    /*
     * 
     */
    
    private static List<InterfaceTestResizer> newResizerList_upscaling() {
        final List<InterfaceTestResizer> ret = new ArrayList<>();
        ret.add(new TestResizerWithScalingType(ScalingType.BICUBIC));
        return ret;
    }
    
    private static List<InterfaceTestResizer> newResizerList_noscaling() {
        final List<InterfaceTestResizer> ret = new ArrayList<>();
        /*
         * Should delegate to this one when there is no resize,
         * so no need to bench other scalings.
         */
        ret.add(new TestResizerWithScalingType(ScalingType.NEAREST));
        return ret;
    }
    
    private static List<InterfaceTestResizer> newResizerList_downscaling() {
        final List<InterfaceTestResizer> ret = new ArrayList<>();
        if (MUST_BENCH_DOWNSCALING_WITH_NEAREST) {
            ret.add(new TestResizerWithScalingType(ScalingType.NEAREST));
        }
        if (MUST_BENCH_DOWNSCALING_WITH_BOXSAMPLED) {
            ret.add(new TestResizerWithScalingType(ScalingType.BOXSAMPLED));
        }
        if (MUST_BENCH_DOWNSCALING_WITH_BILINEAR) {
            ret.add(new TestResizerWithScalingType(ScalingType.BILINEAR));
        }
        if (MUST_BENCH_DOWNSCALING_WITH_BICUBIC) {
            ret.add(new TestResizerWithScalingType(ScalingType.BICUBIC));
        }
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JimsizrPerf() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        newRun();
    }
    
    public static void newRun() {
        new JimsizrPerf().run();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run() {
        final long a = System.nanoTime();
        System.out.println("--- " + JimsizrPerf.class.getSimpleName() + "... ---");
        
        bench_scaleImage_general();
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + JimsizrPerf.class.getSimpleName()
            + ", " + JisTestUtils.toStringTimeMeasureNs(b-a) + " s ---");
    }
    
    /*
     * 
     */
    
    private void bench_scaleImage_general() {
        if (MUST_BENCH_UPSCALING) {
            bench_scaleImage_resizerList(
                newResizerList_upscaling(),
                CALLS_SRCSPAN_DSTSPAN_ARR_UPSCALING);
        }
        if (MUST_BENCH_NOSCALING) {
            bench_scaleImage_resizerList(
                newResizerList_noscaling(),
                CALLS_SRCSPAN_DSTSPAN_ARR_NOSCALING);
        }
        if (MUST_BENCH_DOWNSCALING) {
            bench_scaleImage_resizerList(
                newResizerList_downscaling(),
                CALLS_SRCSPAN_DSTSPAN_ARR_DOWNSCALING);
        }
    }
    
    private void bench_scaleImage_resizerList(
        List<InterfaceTestResizer> resizerList,
        int[][] callsSrcSpanDstSpanArr) {
        
        System.out.println();
        
        for (int[] callsSrcSpanDstSpan : callsSrcSpanDstSpanArr) {
            System.out.println();
            final int nbrOfCalls = CALLS_FACTOR * callsSrcSpanDstSpan[0];
            final int srcSpans = SPAN_FACTOR * callsSrcSpanDstSpan[1];
            final int dstSpans = SPAN_FACTOR * callsSrcSpanDstSpan[2];
            this.bench_scaleImage_xxx(
                srcSpans,
                dstSpans,
                nbrOfCalls,
                BENCHED_SRC_IMAGE_TYPE_ENUM_LIST,
                BENCHED_DST_IMAGE_TYPE_ENUM_LIST,
                resizerList,
                MUST_BENCH_SEQ,
                MUST_BENCH_PRL);
        }
    }
    
    /*
     * 
     */
    
    private void bench_scaleImage_xxx(
        int srcSpans,
        int dstSpans,
        int nbrOfCalls,
        //
        List<TestImageTypeEnum> srcImageTypeEnumList,
        List<TestImageTypeEnum> dstImageTypeEnumList,
        List<InterfaceTestResizer> resizerList,
        boolean mustBenchSeq,
        boolean mustBenchPrl) {
        
        for (TestImageTypeEnum srcImageTypeEnum : srcImageTypeEnumList) {
            final int srcImageType = srcImageTypeEnum.imageType();
            
            final BufferedImage srcImg =
                new BufferedImage(
                    srcSpans,
                    srcSpans,
                    //
                    srcImageType);
            final BufferedImageHelper srcHelper =
                new BufferedImageHelper(srcImg);
            
            for (boolean opaque : new boolean[] {true, false}) {
                if ((!opaque)
                    && srcHelper.isOpaque()) {
                    // N/A
                    continue;
                }
                if ((!opaque)
                    && (!MUST_BENCH_TRANSLUCENT)) {
                    continue;
                }
                
                // To have exactly the same input
                // for each resizer, when parameters are the same.
                final Random random = BihTestUtils.newRandom();
                BihTestUtils.randomizeHelper(
                    random,
                    srcHelper,
                    opaque);
                
                // Separation between input types.
                boolean didSep = false;
                
                /*
                 * 
                 */
                
                for (TestImageTypeEnum dstImageTypeEnum : dstImageTypeEnumList) {
                    final int dstImageType = dstImageTypeEnum.imageType();
                    
                    final BufferedImage dstImg =
                        new BufferedImage(
                            dstSpans,
                            dstSpans,
                            //
                            dstImageType);
                    
                    for (InterfaceTestResizer resizer : resizerList) {
                        
                        // Slower first, faster last (the usual benching order).
                        for (boolean prl : new boolean[] {false, true}) {
                            if (prl ? !mustBenchPrl : !mustBenchSeq) {
                                // N/A
                                continue;
                            }
                            
                            if (!didSep) {
                                System.out.println();
                                didSep = true;
                            }
                            
                            final Executor parallelExecutor =
                                (prl ? PRL_EXECUTOR : null);
                            for (int k = 0; k < NBR_OF_RUNS; k++) {
                                final long a = System.nanoTime();
                                for (int i = 0; i < nbrOfCalls; i++) {
                                    resizer.resize(
                                        srcImg,
                                        dstImg,
                                        parallelExecutor);
                                    if (dstImg.getRGB(0, 0) == 0) {
                                        System.out.println("rare");
                                    }
                                }
                                final long b = System.nanoTime();
                                final String prlStr = "prl=" + (prl ? PRL : "1");
                                final String opacityStr =
                                    (opaque ? "(op)" : "(tr)");
                                System.out.println(nbrOfCalls + " call" + ((nbrOfCalls >= 2) ? "s" : "")
                                    + ", (" + srcSpans + "^2->" + dstSpans + "^2)"
                                    + ", " + opacityStr
                                    + ", " + srcImageTypeEnum.toStringShort()
                                    + "->" + dstImageTypeEnum.toStringShort()
                                    + ", " + resizer
                                    + ", " + prlStr
                                    + ", took "
                                    + JisTestUtils.toStringTimeMeasureNs(b-a)
                                    + " s");
                            }
                        }
                    }
                }
            }
        }
    }
}
