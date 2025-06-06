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
package net.jimsizr.types;

public class ResizeUseCaseIndexUtils {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private ResizeUseCaseIndexUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static int computeResizeUseCaseIndex(
        int srcExtendedImageType,
        int dstExtendedImageType,
        final ResizeAlgoType algoType,
        final ScalingDirAndMag scalingDirAndMag) {
        
        int ret = ExtendedImageTypes.toExtendedImageTypeIndex(srcExtendedImageType);
        //
        ret <<= ExtendedImageTypes.EXTENDED_IMAGE_TYPE_INDEX_BIT_SIZE;
        ret += ExtendedImageTypes.toExtendedImageTypeIndex(dstExtendedImageType);
        //
        ret <<= ResizeAlgoType.ordinalBitSize();
        ret += algoType.ordinal();
        //
        ret <<= ScalingDirAndMag.ordinalBitSize();
        ret += ((scalingDirAndMag != null) ? scalingDirAndMag.ordinal() : 0);
        //
        return ret;
    }
}
