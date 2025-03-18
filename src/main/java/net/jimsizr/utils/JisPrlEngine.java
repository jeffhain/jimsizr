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
package net.jimsizr.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Poor man's parallelization engine, but good enough for our needs
 * (parallelizing rather large chunks of work).
 * 
 * Works well when used with a ThreadPoolExecutor.
 * 
 * ForkJoinPool can also be used, but it might make things slower,
 * since it's not optimized for latency but for throughput,
 * especially when used from a non-worker thread,
 * whereas ThreadPoolExecutor is good at spinning up just the needed amount
 * of workers fast (using chain signaling) and then getting them back to rest
 * quickly.
 */
public class JisPrlEngine {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisPrlEngine() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Eventual RejectedExecutionException is catched,
     * and causes sequential execution of remaining runnables.
     * 
     * @param runnableArr Runnables to run in parallel.
     * @param parallelExecutor Executor for parallelization.
     * @param latch Latch awaited on uninterruptibly before completion.
     * @throws RejectedExecutionException if one has been thrown
     *         by the specified executor.
     */
    public static void parallelRun(
        Runnable[] runnableArr,
        Executor parallelExecutor,
        CountDownLatch latch) {
        
        RejectedExecutionException thrownREE = null;
        for (Runnable runnable : runnableArr) {
            if (thrownREE == null) {
                try {
                    parallelExecutor.execute(runnable);
                } catch (RejectedExecutionException e) {
                    thrownREE = e;
                }
            }
            if (thrownREE != null) {
                // From now on, going sequential.
                runnable.run();
            }
        }
        
        InterruptedException thrownIE = null;
        while (true) {
            try {
                latch.await();
                // Done waiting.
                break;
            } catch (InterruptedException e) {
                // Uninterruptible wait.
                thrownIE = e;
            }
        }
        
        if (thrownIE != null) {
            // Restoring interrupt status.
            Thread.currentThread().interrupt();
        }
        
        if (thrownREE != null) {
            // Throwing a new one, because the related runnable
            // has actually been run.
            throw new RejectedExecutionException(
                "runnable(s) rejected, completed sequentially");
        }
    }
}
