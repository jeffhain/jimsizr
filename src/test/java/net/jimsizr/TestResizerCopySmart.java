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

import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.test.gui.TestResizerWithScaler;
import net.jimsizr.test.utils.InterfaceTestResizer;

public class TestResizerCopySmart extends TestResizerWithScaler {

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerCopySmart(
        int dstAreaThresholdForSplit,
        int maxSliceArea) {
        super(new ScalerCopySmart(
            dstAreaThresholdForSplit,
            maxSliceArea));
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public boolean isCopy() {
        return true;
    }
    
    @Override
    public InterfaceTestResizer withMaxSplitThresholds() {
        return new TestResizerCopySmart(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    }
}
