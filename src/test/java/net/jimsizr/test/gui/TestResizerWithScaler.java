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
package net.jimsizr.test.gui;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.test.utils.InterfaceTestResizer;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Test resizer based on a scaler.
 */
public class TestResizerWithScaler implements InterfaceTestResizer {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceScaler scaler;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestResizerWithScaler(
        InterfaceScaler scaler) {
        this.scaler = scaler;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return this.scaler.toString();
    }
    
    /**
     * Must override if is copy.
     * 
     * @return false
     */
    @Override
    public boolean isCopy() {
        return false;
    }
    
    /**
     * Must override if is not parallel capable.
     * 
     * @return true
     */
    @Override
    public boolean isParallelCapable() {
        return true;
    }

    @Override
    public void resize(
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor) {
        this.scaler.scaleImage(
            new BufferedImageHelper(srcImage),
            new BufferedImageHelper(dstImage),
            parallelExecutor);
    }
    
    @Override
    public InterfaceTestResizer withMaxSplitThresholds() {
        throw new UnsupportedOperationException();
    }
}
