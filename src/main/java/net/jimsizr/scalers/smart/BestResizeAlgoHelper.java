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
package net.jimsizr.scalers.smart;

import net.jimsizr.scalers.smart.gen.BestResizeAlgoArrayGenPrl;
import net.jimsizr.scalers.smart.gen.BestResizeAlgoArrayGenSeq;
import net.jimsizr.types.ExtendedImageTypes;
import net.jimsizr.types.ResizeAlgo;
import net.jimsizr.types.ResizeAlgoType;
import net.jimsizr.types.ResizeUseCaseIndexUtils;
import net.jimsizr.types.ScalingDirAndMag;

public class BestResizeAlgoHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int RESIZE_USE_CASE_INDEX_BIT_SIZE =
        2 * ExtendedImageTypes.EXTENDED_IMAGE_TYPE_INDEX_BIT_SIZE
        + ResizeAlgoType.ordinalBitSize()
        + ScalingDirAndMag.ordinalBitSize();
    private static final int RESIZE_USE_CASE_INDEX_BOUND = ((1 << RESIZE_USE_CASE_INDEX_BIT_SIZE) - 1);
    
    /*
     * 
     */
    
    private static final byte[] SEQ_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX =
        new byte[RESIZE_USE_CASE_INDEX_BOUND];
    static {
        BestResizeAlgoArrayGenSeq.initBestResizeAlgoIndexByResizeUseCaseIndex(
            SEQ_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX);
    }
    
    private static final byte[] PRL_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX =
        new byte[RESIZE_USE_CASE_INDEX_BOUND];
    static {
        BestResizeAlgoArrayGenPrl.initBestResizeAlgoIndexByResizeUseCaseIndex(
            PRL_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX);
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private BestResizeAlgoHelper() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static ResizeAlgo getBestResizeAlgo(
        int srcImageType,
        boolean srcPremul,
        //
        int dstImageType,
        boolean dstPremul,
        //
        ResizeAlgoType algoType,
        //
        int srcWidth,
        int srcHeight,
        int dstWidth,
        int dstHeight,
        //
        boolean prlElseSeq) {
        
        final int srcExtendedImageType =
            ExtendedImageTypes.toExtendedImageType(
                srcImageType,
                srcPremul);
        final int dstExtendedImageType =
            ExtendedImageTypes.toExtendedImageType(
                dstImageType,
                dstPremul);
        
        final ScalingDirAndMag scalingDirAngMag =
            ScalingDirAndMag.valueOfSpans(
                srcWidth,
                srcHeight,
                dstWidth,
                dstHeight);
        
        final int useCaseIndex =
            ResizeUseCaseIndexUtils.computeResizeUseCaseIndex(
                srcExtendedImageType,
                dstExtendedImageType,
                algoType,
                scalingDirAngMag);
        
        final int resizeAlgoIndex;
        if (prlElseSeq) {
            resizeAlgoIndex = PRL_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX[useCaseIndex];
        } else {
            resizeAlgoIndex = SEQ_RESIZE_ALGO_INDEX_BY_RESIZE_USE_CASE_INDEX[useCaseIndex];
        }
        return ResizeAlgo.valueOfIndex(resizeAlgoIndex);
    }
}
