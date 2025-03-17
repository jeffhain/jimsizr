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

import net.jimsizr.scalers.smart.copy.gen.BestCopyAlgoArrayGenPrl;
import net.jimsizr.scalers.smart.copy.gen.BestCopyAlgoArrayGenSeq;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyUseCaseIndexUtils;
import net.jimsizr.types.ExtendedImageTypes;

public class BestCopyAlgoHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int COPY_USE_CASE_INDEX_BIT_SIZE =
        2 * ExtendedImageTypes.EXTENDED_IMAGE_TYPE_INDEX_BIT_SIZE;
    private static final int COPY_USE_CASE_INDEX_BOUND = ((1 << COPY_USE_CASE_INDEX_BIT_SIZE) - 1);
    
    /*
     * 
     */
    
    private static final byte[] SEQ_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX =
        new byte[COPY_USE_CASE_INDEX_BOUND];
    static {
        BestCopyAlgoArrayGenSeq.initBestCopyAlgoIndexByCopyUseCaseIndex(
            SEQ_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX);
    }
    
    private static final byte[] PRL_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX =
        new byte[COPY_USE_CASE_INDEX_BOUND];
    static {
        BestCopyAlgoArrayGenPrl.initBestCopyAlgoIndexByCopyUseCaseIndex(
            PRL_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX);
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private BestCopyAlgoHelper() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static CopyAlgo getBestCopyAlgo(
        int srcImageType,
        boolean srcPremul,
        //
        int dstImageType,
        boolean dstPremul,
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
        
        final int useCaseIndex =
            CopyUseCaseIndexUtils.computeCopyUseCaseIndex(
                srcExtendedImageType,
                dstExtendedImageType);
        
        final int copyAlgoIndex;
        if (prlElseSeq) {
            copyAlgoIndex = PRL_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX[useCaseIndex];
        } else {
            copyAlgoIndex = SEQ_COPY_ALGO_INDEX_BY_COPY_USE_CASE_INDEX[useCaseIndex];
        }
        return CopyAlgo.valueOfIndex(copyAlgoIndex);
    }
}
