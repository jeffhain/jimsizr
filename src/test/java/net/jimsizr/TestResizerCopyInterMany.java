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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.test.gui.TestResizerWithScaler;
import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Resizer using many preliminary intermediary images,
 * with the same image type appearing multiple times,
 * to check correctness of code that deals with that.
 */
public class TestResizerCopyInterMany extends TestResizerWithScaler {

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerCopyInterMany(
        int dstAreaThresholdForSplit,
        int maxSliceArea) {
        super(new ScalerCopySmart(
            dstAreaThresholdForSplit,
            maxSliceArea) {
            @Override
            protected Object computeImplData(
                BufferedImageHelper srcHelper,
                BufferedImageHelper dstHelper,
                Executor parallelExecutor,
                boolean prlElseSeq) {
                
                final List<Integer> preImageTypeList = new ArrayList<Integer>();
                // Writing into both a reused non-premul and a reused premul. 
                preImageTypeList.add(BufferedImage.TYPE_INT_ARGB);
                preImageTypeList.add(BufferedImage.TYPE_INT_ARGB_PRE);
                preImageTypeList.add(BufferedImage.TYPE_INT_ARGB);
                preImageTypeList.add(BufferedImage.TYPE_INT_ARGB_PRE);
                return new Object[] {
                    AlgoBrand.AWT,
                    preImageTypeList};
            }
        });
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
        return new TestResizerCopyInterMany(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    }
}
