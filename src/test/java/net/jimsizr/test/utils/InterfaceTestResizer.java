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
package net.jimsizr.test.utils;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

/**
 * Resizer interface for tests.
 */
public interface InterfaceTestResizer {
    
    /**
     * @return Resizer's name, to identify it.
     */
    @Override
    public String toString();
    
    /**
     * @return True if is copy (i.e. incompatible with actual scaling).
     */
    public boolean isCopy();
    
    /**
     * @return True if is or could be iterative.
     */
    public boolean isIterative();
    
    /**
     * @return True if can benefit from a parallelExecutor.
     */
    public boolean isParallelCapable();
    
    public void resize(
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor);
    
    /**
     * Allows to test eventual inaccuracies introduced by splits.
     * 
     * @return The same algo but with max split thresholds,
     *         to ensure no split whatever image size or parallelism.
     */
    public InterfaceTestResizer withMaxSplitThresholds();
}
