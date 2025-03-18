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

import net.jimsizr.scalers.basic.jis.ScalerBicubicJis;
import net.jimsizr.scalers.implutils.ScalerIterForDown;
import net.jimsizr.test.gui.TestResizerWithScaler;
import net.jimsizr.test.utils.InterfaceTestResizer;

public class TestResizerIterBicuJis extends TestResizerWithScaler {

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerIterBicuJis(int dstAreaThresholdForSplit) {
        super(new ScalerIterForDown(
            new ScalerBicubicJis(
                dstAreaThresholdForSplit)));
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public InterfaceTestResizer withMaxSplitThresholds() {
        return new TestResizerIterBicuJis(Integer.MAX_VALUE);
    }
}
