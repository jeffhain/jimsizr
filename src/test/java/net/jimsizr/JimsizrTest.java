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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import junit.framework.TestCase;
import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

public class JimsizrTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean MUST_PRINT_IMAGE_ON_ERROR = false;
    
    /**
     * True to compute tolerated deltas to copy in JimsizrTestGen,
     * and visually check that they make sense (or debug if not),
     * false to test algos against them (for non-regression,
     * and to test that true parallelism is as accurate
     * as fake parallelism).
     */
    private static final boolean MAX_DELTA_COMPUTATION_MODE = false;
    
    /**
     * When MAX_DELTA_COMPUTATION_MODE is true,
     * if encountering a larger delta (i.e. 0xFF),
     * we fail, because we don't mean to measure accuracy
     * on completely inaccurate operations.
     */
    private static final int MAX_COMPUTED_DELTA = 0xFE;
    
    private static int getCheckNeighborDelta(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake) {
        
        final int checkNeighborDelta;
        if ((cmpType == MyCmpType.WITH_NO_SPLIT)
            || (cmpType == MyCmpType.WITH_REF)) {
            /*
             * AWT/JIS might not round to the same source pixel.
             * With splits on resize, it's worse.
             */
            final boolean gotPixelDeltaDueToSplits =
                (opType != MyOpType.COPY)
                && (prlTrueElseFake != null);
            if (gotPixelDeltaDueToSplits) {
                checkNeighborDelta = 2;
            } else {
                checkNeighborDelta = 1;
            }
        } else {
            checkNeighborDelta = 0;
        }
        return checkNeighborDelta;
    }
    
    /**
     * Should be large enough to cover various pixel cases,
     * and trigger eventual issues (some won't show up if too small).
     * 
     * With growths to x4 for each span, (64,32) gives
     * (256,128) destination images, i.e. images with 32 * 1024 pixels,
     * which should be enough to pass most thresholds.
     * In case it's not enough, we also use small thresholds
     * for tested implementations.
     */
    private static final int SRC_IMAGE_MAX_WIDTH = 64;
    private static final int SRC_IMAGE_MAX_HEIGHT = 32;
    
    /**
     * Only using JIS (our accuracy reference) allows to observe
     * it's usual better accuracy over smart algos, that often
     * delegate to drawImage() usages for speed (even for copies).
     */
    private static final boolean MUST_TEST_SMART_ALGOS = true;
    
    /**
     * Just an internal logic test freak,
     * uselessly slightly inaccurate (np-p-np).
     */
    private static final boolean MUST_TEST_SMART_INTER_MANY_ALGO = true;
    
    /**
     * Max component delta by use case,
     * for tests scenarios done in this class.
     * 
     * If empty, means no constraint.
     */
    private static final SortedMap<String,Integer> MAX_DELTA_BY_CASE;
    static {
        final SortedMap<String,Integer> m =  new TreeMap<>();
        if (MAX_DELTA_COMPUTATION_MODE) {
            // Will compute map thresholds.
            MAX_DELTA_BY_CASE = m;
        } else {
            // Will check that map thresholds are still valid.
            JimsizrTestGen.populateMaxDeltaByCase(m);
            //
            MAX_DELTA_BY_CASE = Collections.unmodifiableSortedMap(m);
        }
    }
    
    private static String computeDeltaCase(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        TestImageTypeEnum srcImageTypeEnum,
        TestImageTypeEnum dstImageTypeEnum) {
        
        final StringBuilder sb = new StringBuilder();
        
        if (cmpType == MyCmpType.WITH_NO_SPLIT) {
            sb.append("vsNoSplit");
        } else if (cmpType == MyCmpType.WITH_REF) {
            sb.append("vsRef");
        } else if (cmpType == MyCmpType.WITH_BACK) {
            sb.append("vsBack");
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        sb.append("-");
        if (opType == MyOpType.COPY) {
            sb.append("copy");
        } else if (opType == MyOpType.RANDOM_NON_UNIFORM) {
            sb.append("resz-nonUni");
        } else {
            sb.append("resz-uni");
        }
        
        sb.append("-");
        final String srcStr = toStringImageTypeCase(srcImageTypeEnum);
        final String dstStr = toStringImageTypeCase(dstImageTypeEnum);
        sb.append("(");
        sb.append(srcStr);
        if (cmpType == MyCmpType.WITH_BACK) {
            sb.append("<->");
        } else {
            sb.append("->");
        }
        sb.append(dstStr);
        sb.append(")");
        
        sb.append("-");
        if (prlTrueElseFake == null) {
            sb.append("seq");
        } else {
            // Computing deltas with "fakePrl",
            // using them for checks for both "fakePrl" and "truePrl".
            sb.append("prl");
        }
        
        return sb.toString();
    }
    
    private static String toStringImageTypeCase(TestImageTypeEnum imageTypeEnum) {
        /*
         * "K" for "kolor", because "C"
         * would locate color types
         * between binary and gray.
         * 
         * To simplify, we can conflate
         * all (RGB,ARGB,ARGB_P) types
         * due to always using high alpha.
         * 
         * That way, we have types alphabetically ordered
         * by somewhat increating accuracy:
         * BIN->GRA->IND->K16->K24
         */
        if (imageTypeEnum.isBinary()) {
            return "BIN";
        }
        if (imageTypeEnum.isGray()) {
            return "GRA";
        }
        if (imageTypeEnum.isIndexed()) {
            return "IND";
        }
        if (imageTypeEnum.isUShortRgb()) {
            return "K16";
        }
        if (imageTypeEnum.isWithAccurateColor()) {
            return "K24";
        }
        throw new IllegalArgumentException("" + imageTypeEnum);
    }
    
    /*
     * 
     */
    
    /**
     * @return Map of reference resizer (possibly null) by resizer to test.
     */
    private Map<InterfaceTestResizer,InterfaceTestResizer> newRefResizerByTestedResizer() {
        // Linked for iteration determinism.
        final Map<InterfaceTestResizer,InterfaceTestResizer> refByTested =
            new LinkedHashMap<>();
        
        /*
         * Never using AWT raw, because can be very inaccurate.
         * Only using JIS, or smart.
         */
        
        final int smallThreshold = Math.min(32, SRC_IMAGE_MAX_WIDTH);
        
        /*
         * Copy.
         */
        
        final InterfaceTestResizer copyRef =
            new TestResizerCopyRef();
        
        final InterfaceTestResizer copyJis =
            new TestResizerCopyJis();
        final InterfaceTestResizer copySmart =
            new TestResizerCopySmart(smallThreshold, smallThreshold);
        final InterfaceTestResizer copySmartMany =
            new TestResizerCopyInterMany(smallThreshold, smallThreshold);
        
        refByTested.put(copyJis, copyRef);
        refByTested.put(copyJis, copyRef);
        if (MUST_TEST_SMART_ALGOS) {
            refByTested.put(copySmart, copyRef);
            //
            if (MUST_TEST_SMART_INTER_MANY_ALGO) {
                refByTested.put(copySmartMany, copyRef);
            }
        }
        
        /*
         * Resizing: JIS raw.
         * 
         * Only tested against themselves,
         * with resize-then-back,
         * since they are our reference.
         */

        final InterfaceTestResizer nullRef = null;
        
        final InterfaceTestResizer resNearJis =
            new TestResizerNearestJis(smallThreshold);
        final InterfaceTestResizer resIterBiliJis =
            new TestResizerIterBiliJis(smallThreshold);
        final InterfaceTestResizer resIterBicuJis =
            new TestResizerIterBicuJis(smallThreshold);
        final InterfaceTestResizer resBoxJis =
            new TestResizerBoxsampledJis(smallThreshold, smallThreshold);
        
        refByTested.put(resNearJis, nullRef);
        refByTested.put(resIterBiliJis, nullRef);
        refByTested.put(resIterBicuJis, nullRef);
        refByTested.put(resBoxJis, nullRef);
        
        /*
         * Resizing: ScalingType (smart).
         * Using JIS as reference.
         */
        
        if (MUST_TEST_SMART_ALGOS) {
            final Jimsizr jimsizr = new Jimsizr(smallThreshold);
            
            /*
             * Single scaling type.
             * No BILINEAR/BILICUBIC because not accurate for big downscaling.
             * No BOXSAMPLED because it's always JIS.
             */
            
            final InterfaceTestResizer resNearSmart =
                new TestResizerWithScalingType(
                    ScalingType.NEAREST,
                    jimsizr);
            refByTested.put(resNearSmart, resNearJis);

            final InterfaceTestResizer resIterBiliSmart =
                new TestResizerWithScalingType(
                    ScalingType.ITERATIVE_BILINEAR,
                    jimsizr);
            refByTested.put(resIterBiliSmart, resIterBiliJis);

            final InterfaceTestResizer resIterBicuSmart =
                new TestResizerWithScalingType(
                    ScalingType.ITERATIVE_BICUBIC,
                    jimsizr);
            refByTested.put(resIterBicuSmart, resIterBicuJis);
            
            /*
             * Bi scaling type.
             * 
             * Only testing these against themselves,
             * with resize-then-back,
             * to check the mixup doesn't go ugly.
             */
            
            for (ScalingType[] scalingType12 : new ScalingType[][] {
                // same algo
                {ScalingType.BOXSAMPLED, ScalingType.BOXSAMPLED},
                // same algo family
                {ScalingType.ITERATIVE_BILINEAR, ScalingType.BILINEAR},
                // usual cases
                {ScalingType.BOXSAMPLED, ScalingType.BICUBIC},
                {ScalingType.ITERATIVE_BILINEAR, ScalingType.BICUBIC},
            }) {
                final ScalingType scalingType1 = scalingType12[0];
                final ScalingType scalingType2 = scalingType12[1];
                
                for (boolean isDownThenUp : new boolean[] {false, true}) {
                    refByTested.put(new TestResizerWithScalingType(
                        scalingType1,
                        scalingType2,
                        isDownThenUp,
                        jimsizr),
                        nullRef);
                }
            }
        }
        
        return refByTested;
    }
    
    /*
     * 
     */
    
    /**
     * Not too small min growth,
     * else need more tolerance (pixels more diluted).
     */
    private static final double MIN_GROWTH = 1.4;
    
    /**
     * More than 2, to go beyond special cases,
     * for growths or shrinkings inferior to 2.
     */
    private static final double MAX_GROWTH = 2.4;
    
    private static final int RANDOM_RESIZE_COUNT = 10;
    
    /*
     * 
     */
    
    /**
     * Not too large else need more tolerance.
     * Large enough for colors to move around.
     */
    private static final int MAX_ORTHO_NEIGHBOR_DELTA = 0x0F;
    
    /**
     * Not too small else color can be altered a lot
     * due to resizing operating in alpha-premultiplied color format.
     */
    private static final int MIN_TRANSLUCENT_ALPHA8 = 0xF0;
    
    /*
     * 
     */
    
    private static boolean gotFormatAccuracyLoss(
        TestImageTypeEnum srcImageTypeEnum,
        TestImageTypeEnum dstImageTypeEnum) {
        if ((!srcImageTypeEnum.isBinary())
            && dstImageTypeEnum.isBinary()) {
            return true;
        }
        if ((!srcImageTypeEnum.isBlackAndWhite())
            && dstImageTypeEnum.isBlackAndWhite()) {
            return true;
        }
        if ((!srcImageTypeEnum.isIndexed())
            && dstImageTypeEnum.isIndexed()) {
            return true;
        }
        if (srcImageTypeEnum.isWithAccurateColor()
            && (!dstImageTypeEnum.isWithAccurateColor())) {
            return true;
        }
        return false;
    }
    
    /**
     * Can use all types of images, will eventually skip later.
     * Not using all CUSTOM types, else takes too long,
     * and just one non-premul and one premul is fine.
     */
    private static final List<TestImageTypeEnum> IMAGE_TYPE_ENUM_LIST =
        JisTestUtils.newImageTypeEnumList(false, true, true, true);
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Initial resize type.
     */
    private enum MyOpType {
        /**
         * dst spans = src spans
         */
        COPY,
        /**
         * Growth in a vicinity of two (which could be
         * a trigger limit for different scaling).
         */
        RANDOM_UNIFORM_UP,
        /**
         * Both growth and shrinking
         * (might be a trigger for different scaling).
         */
        RANDOM_NON_UNIFORM,
        /**
         * Growth a power of two (x2, x4, etc.)
         * (might trigger special cases).
         */
        POWERS_OF_TWO,
    }
    
    private enum MyCmpType {
        WITH_NO_SPLIT,
        WITH_REF,
        WITH_BACK,
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public JimsizrTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_resize_5_exceptions() {
    	this.test_resize_exceptions_xxx(true);
    }
    
    public void test_resize_8_exceptions() {
    	this.test_resize_exceptions_xxx(false);
    }
    
    public void test_resize_exceptions_xxx(boolean resize5Else8) {
    	final BufferedImage srcImage = BihTestUtils.newImageArgb(10, 10);
    	final BufferedImage dstImage = BihTestUtils.newImageArgb(10, 10);
    	
    	try {
    		call_resize_xxx(
    			resize5Else8,
    			//
    			null,
    			ScalingType.NEAREST,
    			srcImage,
    			dstImage,
    			null);
    		fail();
		} catch (NullPointerException e) {
			assertNotNull(e);
		}
    	
    	try {
    		call_resize_xxx(
    			resize5Else8,
    			//
    			ScalingType.NEAREST,
    			null,
    			srcImage,
    			dstImage,
    			null);
    		fail();
		} catch (NullPointerException e) {
			assertNotNull(e);
		}
    	
    	try {
    		call_resize_xxx(
    			resize5Else8,
    			//
    			ScalingType.NEAREST,
    			ScalingType.NEAREST,
    			null,
    			dstImage,
    			null);
    		fail();
		} catch (NullPointerException e) {
			assertNotNull(e);
		}
    	
    	try {
    		call_resize_xxx(
    			resize5Else8,
    			//
    			ScalingType.NEAREST,
    			ScalingType.NEAREST,
    			srcImage,
    			null,
    			null);
    		fail();
		} catch (NullPointerException e) {
			assertNotNull(e);
		}
    	
    	// same image
    	try {
    		call_resize_xxx(
    			resize5Else8,
    			//
    			ScalingType.NEAREST,
    			ScalingType.NEAREST,
    			srcImage,
    			srcImage,
    			null);
    		fail();
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
    }
    
    /*
     * 
     */
    
    public void test_copy_vsWithNoSplit_seq() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_NO_SPLIT, null);
    }
    
    public void test_copy_vsWithNoSplit_fakePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_NO_SPLIT, false);
    }
    
    public void test_copy_vsWithNoSplit_truePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_NO_SPLIT, true);
    }
    
    public void test_copy_vsWithRef_seq() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, null);
    }
    
    public void test_copy_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, false);
    }
    
    public void test_copy_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_REF, true);
    }
    
    public void test_copy_vsWithBack_seq() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_BACK, null);
    }
    
    public void test_copy_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_BACK, false);
    }
    
    public void test_copy_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.COPY, MyCmpType.WITH_BACK, true);
    }
    
    public void test_resize_randomUniform_vsWithNoSplit_seq() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_NO_SPLIT, null);
    }
    
    public void test_resize_randomUniform_vsWithNoSplit_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_NO_SPLIT, false);
    }
    
    public void test_resize_randomUniform_vsWithNoSplit_truePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_NO_SPLIT, true);
    }
    
    public void test_resize_randomUniform_vsWithRef_seq() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_randomUniform_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_randomUniform_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_randomUniform_vsWithBack_seq() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_randomUniform_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_randomUniform_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.RANDOM_UNIFORM_UP, MyCmpType.WITH_BACK, true);
    }
    
    public void test_resize_randomNonUniform_vsWithNoSplit_seq() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_NO_SPLIT, null);
    }
    
    public void test_resize_randomNonUniform_vsWithNoSplit_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_NO_SPLIT, false);
    }
    
    public void test_resize_randomNonUniform_vsWithNoSplit_truePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_NO_SPLIT, true);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_seq() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_randomNonUniform_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_seq() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_randomNonUniform_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.RANDOM_NON_UNIFORM, MyCmpType.WITH_BACK, true);
    }
    
    public void test_resize_powersOfTwo_vsWithNoSplit_seq() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_NO_SPLIT, null);
    }
    
    public void test_resize_powersOfTwo_vsWithNoSplit_fakePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_NO_SPLIT, false);
    }
    
    public void test_resize_powersOfTwo_vsWithNoSplit_truePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_NO_SPLIT, true);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_seq() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, null);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_fakePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, false);
    }
    
    public void test_resize_powersOfTwo_vsWithRef_truePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_REF, true);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_seq() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, null);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_fakePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, false);
    }
    
    public void test_resize_powersOfTwo_vsWithBack_truePrl() {
        this.test_xxx(MyOpType.POWERS_OF_TWO, MyCmpType.WITH_BACK, true);
    }
    
    /*
     * 
     */
    
    /**
     * False parallelism allows to ensure sequential and identical
     * ordering of parallel runnables executions, and deterministic
     * triggering of split code coordinates issues.
     * True parallelism allows to trigger eventual concurrency bugs.
     * 
     * @param prlTrueElseFake True to use actual parallelism,
     *        false to use an executor with a single thread,
     *        null for sequential.
     */
    public void test_xxx(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake) {
        
        final Executor parallelExecutor;
        if (prlTrueElseFake != null) {
            // 2 for easier debug.
            final int parallelism = (prlTrueElseFake ? 2 : 1);
            parallelExecutor = JisTestUtils.newPrlExec(parallelism);
        } else {
            parallelExecutor = null;
        }
        try {
            this.test_xxx_exec(opType, cmpType, prlTrueElseFake, parallelExecutor);
        } finally {
            JisTestUtils.shutdownNow(parallelExecutor);
        }
    }
    
    public void test_xxx_exec(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        Executor parallelExecutor) {
        
        final Random random = BihTestUtils.newRandom();
        
        for (Map.Entry<InterfaceTestResizer,InterfaceTestResizer> entry :
            newRefResizerByTestedResizer().entrySet()) {
            final InterfaceTestResizer resizer = entry.getKey();
            final InterfaceTestResizer refResizer = entry.getValue();
            
            if (resizer.isCopy() != (opType == MyOpType.COPY)) {
                /*
                 * If resizer is copy and we have resize,
                 * won't work so skipping.
                 * If resizer is not copy and we have no resize,
                 * can't happen in real Jimsizr usage (since it
                 * delegates to actual copy if no resize),
                 * and it could cause errors due to np-p-np conversion,
                 * so skipping as well.
                 */
                continue;
            }
            
            for (TestImageTypeEnum srcImageTypeEnum : IMAGE_TYPE_ENUM_LIST) {
                final int srcWidth =
                    1 + random.nextInt(SRC_IMAGE_MAX_WIDTH);
                final int srcHeight =
                    1 + random.nextInt(SRC_IMAGE_MAX_HEIGHT);
                final BufferedImage srcImage =
                    BihTestUtils.newImage(
                        srcWidth,
                        srcHeight,
                        srcImageTypeEnum);
                
                // If src has alpha, always having some translucent pixels.
                JisTestUtils.randomizeCloseNeighboors(
                    random,
                    srcImage,
                    MAX_ORTHO_NEIGHBOR_DELTA,
                    false,
                    MIN_TRANSLUCENT_ALPHA8);
                
                for (TestImageTypeEnum dstImageTypeEnum : IMAGE_TYPE_ENUM_LIST) {
                    
                    final int myResizeCount;
                    if (opType == MyOpType.COPY) {
                        // Just need that.
                        myResizeCount = 1;
                    } else if (opType == MyOpType.POWERS_OF_TWO) {
                        // Need just that.
                        myResizeCount = 4;
                    } else {
                        myResizeCount = RANDOM_RESIZE_COUNT;
                    }
                    
                    for (int rk = 0; rk < myResizeCount; rk++) {
                        final int dstWidth;
                        final int dstHeight;
                        if (opType == MyOpType.COPY) {
                            dstWidth = srcWidth;
                            dstHeight = srcHeight;
                        } else if (opType == MyOpType.RANDOM_UNIFORM_UP) {
                            dstWidth = newGrownSpan(random, srcWidth);
                            dstHeight = newGrownSpan(random, srcHeight);
                        } else if (opType == MyOpType.RANDOM_NON_UNIFORM) {
                            if (random.nextBoolean()) {
                                dstWidth = newGrownSpan(random, srcWidth);
                                dstHeight = newShrinkedSpan(random, srcHeight);
                            } else {
                                dstWidth = newShrinkedSpan(random, srcWidth);
                                dstHeight = newGrownSpan(random, srcHeight);
                            }
                        } else if (opType == MyOpType.POWERS_OF_TWO) {
                            // Covering all x2/x4 cases with rk in 0..3
                            // (x4 makes test slow, so better not do it
                            // over and over).
                            dstWidth = (srcWidth << (1 << (rk/2)));
                            dstHeight = (srcHeight << (1 << (rk%2)));
                        } else {
                            throw new AssertionError();
                        }
                        
                        if (DEBUG) {
                            System.out.println();
                            System.out.println("opType = " + opType);
                            System.out.println("cmpType = " + cmpType);
                            System.out.println("prlTrueElseFake = " + prlTrueElseFake);
                            System.out.println("parallelExecutor = " + parallelExecutor);
                            System.out.println("resizer = " + resizer);
                            System.out.println("refResizer = " + refResizer);
                            System.out.println("srcImageTypeEnum = " + srcImageTypeEnum);
                            System.out.println("dstImageTypeEnum = " + dstImageTypeEnum);
                            System.out.println("srcImage = " + toStringInfo(srcImage));
                            System.out.println("dstWidth = " + dstWidth);
                            System.out.println("dstHeight = " + dstHeight);
                        }
                        
                        if ((cmpType == MyCmpType.WITH_REF)
                            && (refResizer == null)) {
                            // N/A
                            continue;
                        }
                        if ((cmpType == MyCmpType.WITH_BACK)
                            && (gotFormatAccuracyLoss(
                                srcImageTypeEnum,
                                dstImageTypeEnum)
                                || gotFormatAccuracyLoss(
                                    dstImageTypeEnum,
                                    srcImageTypeEnum))) {
                            // Too inaccurate.
                            continue;
                        }
                        
                        test_yyy(
                            opType,
                            cmpType,
                            prlTrueElseFake,
                            resizer,
                            refResizer,
                            srcImageTypeEnum,
                            srcImage,
                            dstImageTypeEnum,
                            dstWidth,
                            dstHeight,
                            parallelExecutor);
                    }
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void tearDown() {
        if (MAX_DELTA_COMPUTATION_MODE) {
            /*
             * Use the last log, after all tests ran.
             */
            if (MAX_DELTA_BY_CASE.size() != 0) {
                System.out.println();
                System.out.println("MAX_DELTA_BY_CASE:");
                for (Map.Entry<String,Integer> e : MAX_DELTA_BY_CASE.entrySet()) {
                    final String deltaCase = e.getKey();
                    final int maxDelta = e.getValue();
                    System.out.println("m.put(\"" + deltaCase + "\", " + maxDelta + ");");
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void call_resize_xxx(
    	boolean resize5Else8,
    	//
    	ScalingType scalingType1,
        ScalingType scalingType2,
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor) {
    	
    	if (resize5Else8) {
    		Jimsizr.resize(
    			scalingType1,
    			scalingType2,
    			srcImage,
    			dstImage,
    			parallelExecutor);
    	} else {
    		Jimsizr.resize(
    			scalingType1,
    			scalingType2,
    			srcImage,
    			dstImage,
    			parallelExecutor,
    			//
    			false,
    			false,
    			false);
    	}
    }
    
    /*
     * 
     */
    
    /**
     * Tests that resizing and then resizing back to initial spans
     * is close to identity.
     * 
     * This allows to test somewhat accurate scaling
     * in a generic way, both for growth and shrinking
     * (assuming the chance for a bug in both cancelling itself
     * being low).
     */
    private void test_yyy(
        MyOpType opType,
        MyCmpType cmpType,
        Boolean prlTrueElseFake,
        //
        InterfaceTestResizer resizer,
        InterfaceTestResizer refResizer,
        TestImageTypeEnum srcImageTypeEnum,
        final BufferedImage srcImage,
        TestImageTypeEnum dstImageTypeEnum,
        int dstWidth,
        int dstHeight,
        Executor parallelExecutor) {
        
        final int srcWidth = srcImage.getWidth();
        final int srcHeight = srcImage.getHeight();
        
        final String deltaCase = computeDeltaCase(
            opType,
            cmpType,
            prlTrueElseFake,
            srcImageTypeEnum,
            dstImageTypeEnum);
        
        // If null, means must compare with back.
        final InterfaceTestResizer cmpResizer;
        if (cmpType == MyCmpType.WITH_NO_SPLIT) {
            cmpResizer = resizer.withMaxSplitThresholds();
        } else if (cmpType == MyCmpType.WITH_REF) {
            cmpResizer = JisUtils.requireNonNull(refResizer);
        } else if (cmpType == MyCmpType.WITH_BACK) {
            cmpResizer = null;
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        /*
         * Always the same output when possible,
         * to be fair between resizers and use cases.
         * For example, allows not to have max delta
         * larger for sequential than parallel,
         * while only parallelism might degrade quality
         * due to images splits that would not be well managed.
         */
        final Random randomForOutput = BihTestUtils.newRandom();
        
        /*
         * Resizing.
         */
        
        final int dstScanlineStride = dstWidth + 1;
        final BufferedImage dstImage =
            BihTestUtils.newImage(
                dstScanlineStride,
                dstWidth,
                dstHeight,
                dstImageTypeEnum);
        final BufferedImageHelper dstHelper =
            new BufferedImageHelper(dstImage);
        final BufferedImage cmpImage =
            (cmpResizer == null) ? null
                : BihTestUtils.newImage(
                    dstScanlineStride,
                    dstWidth,
                    dstHeight,
                    dstImageTypeEnum);
        final BufferedImageHelper cmpHelper =
            (cmpResizer == null) ? null
                : new BufferedImageHelper(cmpImage);
        // Randomizing output, must be erased by resize.
        BihTestUtils.randomizeHelper(
            randomForOutput,
            dstHelper,
            false);
        if (cmpResizer != null) {
            JisTestUtils.copyImage_reference(
                dstHelper,
                cmpHelper);
        }
        //
        if (DEBUG) {
            System.out.println();
            System.out.println("resizer.resize("
                + toStringInfo(srcImage)
                + ","
                + toStringInfo(dstImage)
                + ","
                + parallelExecutor
                + ")");
        }
        resizer.resize(
            srcImage,
            dstImage,
            parallelExecutor);
        
        /*
         * 
         */
        
        final BufferedImage expImage;
        final BufferedImage actImage;
        if ((cmpType == MyCmpType.WITH_NO_SPLIT)
            || (cmpType == MyCmpType.WITH_REF)) {
            if (DEBUG) {
                System.out.println();
                System.out.println("cmpResizer.resize("
                    + toStringInfo(srcImage)
                    + ","
                    + toStringInfo(cmpImage)
                    + ","
                    + parallelExecutor
                    + ")");
            }
            /*
             * Actual algo with split (AWT with low prl/slice thresholds),
             * compared to either:
             * - WITH_NO_SPLIT: algo with no split (AWT with max thresholds).
             * - WITH_REF: algo accurate even in case of splits (test reference, or JIS).
             */
            cmpResizer.resize(
                srcImage,
                cmpImage,
                parallelExecutor);
            expImage = cmpImage;
            actImage = dstImage;
        } else if (cmpType == MyCmpType.WITH_BACK) {
            /*
             * Resizing back.
             */
            
            final int backScanlineStride = srcWidth + 1;
            final BufferedImage backImage =
                BihTestUtils.newImage(
                    backScanlineStride,
                    srcWidth,
                    srcHeight,
                    srcImageTypeEnum);
            final BufferedImageHelper backHelper =
                new BufferedImageHelper(backImage);
            // Randomizing output, must be erased by resize.
            BihTestUtils.randomizeHelper(
                randomForOutput,
                backHelper,
                false);
            //
            if (DEBUG) {
                System.out.println();
                System.out.println("(back) resizer.resize("
                    + toStringInfo(dstImage)
                    + ","
                    + toStringInfo(backImage)
                    + ","
                    + parallelExecutor
                    + ")");
            }
            resizer.resize(
                dstImage,
                backImage,
                parallelExecutor);
            
            expImage = srcImage;
            actImage = backImage;
        } else {
            throw new IllegalArgumentException("" + cmpType);
        }
        
        /*
         * 
         */
        
        final int checkNeighborDelta =
            getCheckNeighborDelta(
                opType,
                cmpType,
                prlTrueElseFake);

        /*
         * 
         */
        
        final BufferedImageHelper expHelper = new BufferedImageHelper(expImage);
        final BufferedImageHelper actHelper = new BufferedImageHelper(actImage);
        
        final int maxCptDelta =
            BihTestUtils.computeMaxCptDelta(
                expHelper,
                actHelper,
                checkNeighborDelta);
        
        /*
         * 
         */
        
        // If null, means no constraint.
        final Integer cptDeltaTolRef =
            (MAX_DELTA_COMPUTATION_MODE
                ? MAX_COMPUTED_DELTA : MAX_DELTA_BY_CASE.get(deltaCase));
        
        if ((cptDeltaTolRef != null)
            && (maxCptDelta > cptDeltaTolRef)) {
            final int cptDeltaTol = cptDeltaTolRef;
            System.out.println();
            if (MUST_PRINT_IMAGE_ON_ERROR) {
                final BufferedImageHelper srcHelper =
                    new BufferedImageHelper(srcImage);
                if (cmpType == MyCmpType.WITH_BACK) {
                    BihTestUtils.printImage("dstHelper", dstHelper);
                    BihTestUtils.printImageDiff(
                        "expHelper(src)",
                        expHelper,
                        "actHelper(back)",
                        actHelper);
                } else {
                    BihTestUtils.printImage("srcHelper", srcHelper);
                    BihTestUtils.printImageDiff(
                        "expHelper(cmp)",
                        expHelper,
                        "actHelper(dst)",
                        actHelper);
                }
            }
            System.out.println("opType = " + opType);
            System.out.println("cmpType = " + cmpType);
            System.out.println("prlTrueElseFake = " + prlTrueElseFake);
            System.out.println("deltaCase = " + deltaCase);
            System.out.println("resizer = " + resizer);
            System.out.println("refResizer = " + refResizer);
            System.out.println("cmpResizer = " + cmpResizer);
            System.out.println("srcImageTypeEnum = " + srcImageTypeEnum);
            System.out.println("dstImageTypeEnum = " + dstImageTypeEnum);
            System.out.println("srcImage = " + toStringInfo(srcImage));
            System.out.println("dstImage = " + toStringInfo(dstImage));
            System.out.println("cptDeltaTol = " + cptDeltaTol
                + " = 0x" + Integer.toHexString(cptDeltaTol));
            System.out.println("maxCptDelta = " + maxCptDelta
                + " = 0x" + Integer.toHexString(maxCptDelta));
            fail("not good enough");
        }
        
        /*
         * Only computing delta for sequential and fake parallel cases,
         * not to let delta due to concurrency issue slip into
         * tolerated deltas.
         * Fake parallel case can have different delta than sequential case,
         * because smart treatments might use different algo implementations
         * depending on whether there is a parallel executor or not.
         */
        if (MAX_DELTA_COMPUTATION_MODE
            && ((prlTrueElseFake == null)
                || (!prlTrueElseFake))) {
            Integer prevRef = MAX_DELTA_BY_CASE.get(deltaCase);
            if ((prevRef == null)
                || (maxCptDelta > prevRef.intValue())) {
                MAX_DELTA_BY_CASE.put(deltaCase, maxCptDelta);
            }
        }
    }
    
    /*
     * 
     */
    
    private static String toStringInfo(BufferedImage image) {
        return new BufferedImageHelper(image, false, false).toString();
    }
    
    /*
     * 
     */
    
    private static int newGrownSpan(Random random, int span) {
        final double growth = JisTestUtils.randomMinMax(random, MIN_GROWTH, MAX_GROWTH);
        return (int) Math.rint(span * growth);
    }
    
    private static int newShrinkedSpan(Random random, int span) {
        final double shrinking = JisTestUtils.randomMinMax(random, MIN_GROWTH, MAX_GROWTH);
        return Math.max(1, (int) Math.rint(span / shrinking));
    }
}
