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

import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.test.gui.TestResizerWithScaler;
import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.utils.BufferedImageHelper;

public class TestResizerCopyRef extends TestResizerWithScaler {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyRefCopyScaler implements InterfaceScaler {

        @Override
        public void scaleImage(
            BufferedImageHelper srcHelper,
            BufferedImageHelper dstHelper,
            Executor parallelExecutor) {
            JisTestUtils.copyImage_reference(srcHelper, dstHelper);
        }
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerCopyRef() {
        super(new MyRefCopyScaler());
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isCopy() {
        return true;
    }
    
    @Override
    public InterfaceTestResizer withMaxSplitThresholds() {
        return new TestResizerCopyRef();
    }
}
