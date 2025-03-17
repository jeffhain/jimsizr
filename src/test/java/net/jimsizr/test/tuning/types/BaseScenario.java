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
package net.jimsizr.test.tuning.types;

import net.jimsizr.test.utils.TestImageTypeEnum;
import net.jimsizr.types.CopyUseCaseIndexUtils;
import net.jimsizr.types.ExtendedImageTypes;

/**
 * Base scenario, enough for copy case.
 */
public class BaseScenario {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final TestImageTypeEnum srcImageTypeEnum;
    private final TestImageTypeEnum dstImageTypeEnum;
    private final boolean prlElseSeq;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BaseScenario(
        final TestImageTypeEnum srcImageTypeEnum,
        final TestImageTypeEnum dstImageTypeEnum,
        final boolean prlElseSeq) {
        this.srcImageTypeEnum = srcImageTypeEnum;
        this.dstImageTypeEnum = dstImageTypeEnum;
        this.prlElseSeq = prlElseSeq;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "["
            + this.srcImageTypeEnum.toStringShort()
            + "->"
            + this.dstImageTypeEnum.toStringShort()
            + ", "
            + (this.prlElseSeq ? "prl" : "seq")
            + "]";
    }
    
    /**
     * Without prlOrSeq (separate code generation).
     */
    public String toStringForCodeGen() {
        return this.srcImageTypeEnum.toStringShort()
            + "->"
            + this.dstImageTypeEnum.toStringShort();
    }
    
    /**
     * @return Copy use case index.
     */
    public int computeUseCaseIndex() {
        final int srcExtendedImageType = ExtendedImageTypes.toExtendedImageType(
            this.srcImageTypeEnum.imageType(),
            this.srcImageTypeEnum.isPremul());
        final int dstExtendedImageType = ExtendedImageTypes.toExtendedImageType(
            this.dstImageTypeEnum.imageType(),
            this.dstImageTypeEnum.isPremul());
        return CopyUseCaseIndexUtils.computeCopyUseCaseIndex(
            srcExtendedImageType,
            dstExtendedImageType);
    }

    public TestImageTypeEnum getSrcImageTypeEnum() {
        return this.srcImageTypeEnum;
    }

    public TestImageTypeEnum getDstImageTypeEnum() {
        return this.dstImageTypeEnum;
    }

    public boolean isPrlElseSeq() {
        return this.prlElseSeq;
    }
}
