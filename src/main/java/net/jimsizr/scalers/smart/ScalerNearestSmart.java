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
package net.jimsizr.scalers.smart;

import net.jimsizr.scalers.basic.awt.ScalerNearestAwt;
import net.jimsizr.scalers.basic.jis.ScalerNearestJis;
import net.jimsizr.types.ResizeAlgoType;

public class ScalerNearestSmart extends SmartResizeScaler {

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ScalerNearestSmart() {
        super(
            ResizeAlgoType.NEAREST,
            new ScalerNearestAwt(),
            new ScalerNearestJis());
    }
    
    public ScalerNearestSmart(int dstAreaThresholdForSplit) {
        super(
            ResizeAlgoType.NEAREST,
            new ScalerNearestAwt(dstAreaThresholdForSplit),
            new ScalerNearestJis(dstAreaThresholdForSplit));
    }
}
