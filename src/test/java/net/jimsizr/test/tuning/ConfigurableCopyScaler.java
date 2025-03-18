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
package net.jimsizr.test.tuning;

import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyInterKinds;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

/**
 * Useful for tests, to bench particular algos
 * or to copy with a specific algo.
 */
public class ConfigurableCopyScaler extends ScalerCopySmart {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Corresponds to BufferedImageHelper.copyImage().
     */
    public static final CopyAlgo DEFAULT_ALGO =
        CopyAlgo.valueOf(CopyInterKinds.NONE, AlgoBrand.JIS);
    
    private CopyAlgo copyAlgo = DEFAULT_ALGO;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * Uses BufferedImageHelper.copyImage() as initial copy algo.
     */
    public ConfigurableCopyScaler() {
        this(DEFAULT_ALGO);
    }
    
    /**
     * @param initialCopyAlgo Must not be null.
     */
    public ConfigurableCopyScaler(CopyAlgo initialCopyAlgo) {
        this.copyAlgo = JisUtils.requireNonNull(initialCopyAlgo);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param copyAlgo Must not be null.
     */
    public void configure(CopyAlgo copyAlgo) {
        this.copyAlgo = JisUtils.requireNonNull(copyAlgo);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected CopyAlgo getCopyAlgo(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        boolean prlElseSeq) {
        
        return this.copyAlgo;
    }
}
