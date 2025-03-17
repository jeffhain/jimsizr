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

import java.util.TreeSet;

import junit.framework.TestCase;

public class ResizeUseCaseIndexUtilsTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ResizeUseCaseIndexUtilsTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_computeResizeUseCaseIndex_unicity() {
        
        final TreeSet<Integer> indexSet = new TreeSet<>();
        
        for (int srcExtendedImageType : ExtendedImageTypes.EXTENDED_IMAGE_TYPE_SET) {
            for (int dstExtendedImageType : ExtendedImageTypes.EXTENDED_IMAGE_TYPE_SET) {
                for (ResizeAlgoType algoType : ResizeAlgoType.values()) {
                    for (ScalingDirAndMag scalingDirAndMag : ScalingDirAndMag.values()) {
                        final int index =
                            ResizeUseCaseIndexUtils.computeResizeUseCaseIndex(
                                srcExtendedImageType,
                                dstExtendedImageType,
                                algoType,
                                scalingDirAndMag);
                        final boolean didAdd = indexSet.add(index);
                        if (!didAdd) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }
    }
    
    public void test_computeScalingDirAndMag_inRange() {
        
        final int srcWidth = 1000;
        final int srcHeight = 2000;
        for (ScalingMag expectedMag : ScalingMag.values()) {
            final int spanRefScalingMag = expectedMag.getSpanRefScalingMag();
            for (double spanScalingMag : new double[] {
                spanRefScalingMag * (0.75 * 1.005),
                spanRefScalingMag,
                spanRefScalingMag * (1.5 * 0.995),
            }) {
                for (boolean upElseDown : new boolean[] {false, true}) {
                    if (spanScalingMag <= 1.0) {
                        // N/A: direction defined by "upElseDown".
                        continue;
                    }
                    final double factor = (upElseDown ? spanScalingMag : 1.0 / spanScalingMag);
                    final int dstWidth = (int) (factor * srcWidth);
                    final int dstHeight = (int) (factor * srcHeight);
                    final ScalingDirAndMag actualDirAndMag =
                        ScalingDirAndMag.valueOfSpans(
                            srcWidth,
                            srcHeight,
                            dstWidth,
                            dstHeight);
                    assertEquals(upElseDown, actualDirAndMag.isUpElseDown());
                    assertEquals(expectedMag, actualDirAndMag.getMag());
                }
            }
        }
    }
    
    public void test_computeScalingDirAndMag_outOfRange() {
        
        final int srcWidth = 1000;
        final int srcHeight = 2000;
 
        for (boolean upElseDown : new boolean[] {false, true}) {
            final double huge = 100.0;
            final double factor = (upElseDown ? huge : 1.0 / huge);
            final int dstWidth = (int) (factor * srcWidth);
            final int dstHeight = (int) (factor * srcHeight);
            final ScalingDirAndMag actualDirAnd =
                ScalingDirAndMag.valueOfSpans(
                    srcWidth,
                    srcHeight,
                    dstWidth,
                    dstHeight);
            assertEquals(upElseDown, actualDirAnd.isUpElseDown());
            assertEquals(ScalingMag.M8, actualDirAnd.getMag());
        }
    }
}
