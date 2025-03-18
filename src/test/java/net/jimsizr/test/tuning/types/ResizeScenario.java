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
import net.jimsizr.types.ExtendedImageTypes;
import net.jimsizr.types.ResizeAlgoType;
import net.jimsizr.types.ResizeUseCaseIndexUtils;
import net.jimsizr.types.ScalingDirAndMag;

/**
 * All settings except intermediary images.
 */
public class ResizeScenario extends BaseScenario {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final ResizeAlgoType algoType;
    private final ScalingDirAndMag dirAndMag;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ResizeScenario(
        TestImageTypeEnum srcImageTypeEnum,
        TestImageTypeEnum dstImageTypeEnum,
        boolean prlElseSeq,
        ResizeAlgoType algoType,
        ScalingDirAndMag dirAndMag) {
        super(srcImageTypeEnum, dstImageTypeEnum, prlElseSeq);
        this.algoType = algoType;
        this.dirAndMag = dirAndMag;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "["
            + this.getSrcImageTypeEnum().toStringShort()
            + "->"
            + this.getDstImageTypeEnum().toStringShort()
            + ", "
            + this.algoType.toStringShortest()
            + ", "
            + this.dirAndMag
            + ", "
            + (this.isPrlElseSeq() ? "prl" : "seq")
            + "]";
    }
    
    @Override
    public String toStringForCodeGen() {
        return this.getSrcImageTypeEnum().toStringShort()
            + "->"
            + this.getDstImageTypeEnum().toStringShort()
            + ", "
            + this.algoType.toStringShortest()
            + ", "
            + this.dirAndMag;
    }
    
    /**
     * @return Resize use case index.
     */
    @Override
    public int computeUseCaseIndex() {
        final int srcExtendedImageType = ExtendedImageTypes.toExtendedImageType(
            this.getSrcImageTypeEnum().imageType(),
            this.getSrcImageTypeEnum().isPremul());
        final int dstExtendedImageType = ExtendedImageTypes.toExtendedImageType(
            this.getDstImageTypeEnum().imageType(),
            this.getDstImageTypeEnum().isPremul());
        return ResizeUseCaseIndexUtils.computeResizeUseCaseIndex(
            srcExtendedImageType,
            dstExtendedImageType,
            this.algoType,
            this.dirAndMag);
    }
    
    public ResizeAlgoType getAlgoType() {
        return this.algoType;
    }
    
    public ScalingDirAndMag getDirAndMag() {
        return this.dirAndMag;
    }
}
