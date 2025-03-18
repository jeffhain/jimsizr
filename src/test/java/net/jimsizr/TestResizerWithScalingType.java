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
import java.util.concurrent.Executor;

import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.utils.JisUtils;

public class TestResizerWithScalingType implements InterfaceTestResizer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final ScalingType scalingType1;
    private final ScalingType scalingType2;
    private final boolean isDownThenUp;
    
    private final Jimsizr jimsizr;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerWithScalingType(ScalingType scalingType) {
        this(scalingType, null, false);
    }
    
    /**
     * @param scalingType1 Must not be null.
     * @param scalingType2 Can be null.
     */
    public TestResizerWithScalingType(
        ScalingType scalingType1,
        ScalingType scalingType2,
        boolean isDownThenUp) {
        this(
            scalingType1,
            scalingType2,
            isDownThenUp,
            new Jimsizr(-1));
    }
    
    /*
     * 
     */
    
    /**
     * @param scalingType Must not be null.
     * @param jimsizr Must not be null.
     */
    TestResizerWithScalingType(
        ScalingType scalingType,
        Jimsizr jimsizr) {
        this(
            scalingType,
            scalingType,
            false, // not used
            jimsizr);
    }
    
    /**
     * @param scalingType1 Must not be null.
     * @param scalingType2 Can be null.
     * @param jimsizr Must not be null.
     */
    TestResizerWithScalingType(
        ScalingType scalingType1,
        ScalingType scalingType2,
        boolean isDownThenUp,
        Jimsizr jimsizr) {
        this.scalingType1 = JisUtils.requireNonNull(scalingType1);
        this.scalingType2 = scalingType2;
        this.isDownThenUp = isDownThenUp;
        
        this.jimsizr = JisUtils.requireNonNull(jimsizr);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (this.scalingType2 == null) {
            sb.append(this.scalingType1.toStringShort());
        } else {
            if (this.isDownThenUp) {
                sb.append("down-");
                sb.append(this.scalingType1.toStringShort());
                sb.append(",up-");
                sb.append(this.scalingType2.toStringShort());
            } else {
                sb.append("pre-");
                sb.append(this.scalingType1.toStringShort());
                sb.append(",rem-");
                sb.append(this.scalingType2.toStringShort());
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public boolean isCopy() {
        return false;
    }
    
    @Override
    public boolean isIterative() {
        // Second one doesn't count.
        return (this.scalingType1 == ScalingType.ITERATIVE_BILINEAR)
            || (this.scalingType1 == ScalingType.ITERATIVE_BICUBIC);
    }
    
    @Override
    public boolean isParallelCapable() {
        return true;
    }
    
    @Override
    public void resize(
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor) {
        /*
         * Not bothering to test without array direct use,
         * BufferedImageHelper should behave the same
         * and it's tested in BufferedImageHelper tests.
         */
        final boolean allowArrDirectUse = true;
        this.jimsizr.resize_pp(
            this.scalingType1,
            ((this.scalingType2 != null) ? this.scalingType2 : this.scalingType1),
            srcImage,
            dstImage,
            parallelExecutor,
            this.isDownThenUp,
            allowArrDirectUse,
            allowArrDirectUse);
    }
    
    @Override
    public InterfaceTestResizer withMaxSplitThresholds() {
        return new TestResizerWithScalingType(
            this.scalingType1,
            this.scalingType2,
            this.isDownThenUp,
            new Jimsizr(Integer.MAX_VALUE));
    }
}
