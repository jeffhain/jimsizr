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
package net.jimsizr.scalers.api;

import java.util.concurrent.Executor;

import net.jimsizr.utils.BufferedImageHelper;

public interface InterfaceScaler {

    /**
     * We use BufferedImageHelper and not BufferedImage as arguments,
     * not to clutter this method or scaler constructors with
     * BufferedImageHelper configuration parameters,
     * and for instance reuse across successive scalings,
     * to avoid the overhead of creating a new one every time.
     * 
     * @param srcHelper Source image helper.
     * @param dstHelper Destination image helper.
     * @param parallelExecutor If not null, used for parallelization
     *        (so should have at least two worker threads).
     */
    public void scaleImage(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor);
}
