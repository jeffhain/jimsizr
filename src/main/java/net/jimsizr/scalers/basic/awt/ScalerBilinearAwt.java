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
package net.jimsizr.scalers.basic.awt;

import java.awt.RenderingHints;

public class ScalerBilinearAwt extends BaseScalerAwt {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT = 2 * 1024;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerBilinearAwt() {
        this(DEFAULT_DST_AREA_THRESHOLD_FOR_SPLIT);
    }
    
    public ScalerBilinearAwt(int dstAreaThresholdForSplit) {
        super(
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            dstAreaThresholdForSplit);
    }
}
