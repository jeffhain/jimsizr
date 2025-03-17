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
package net.jimsizr.test.tuning.types;

import net.jimsizr.test.utils.JisTestUtils;
import net.jimsizr.types.InterfaceAlgo;

/**
 * Measurement of an algo performances (timing and accuracy).
 */
public class AlgoMeas implements Comparable<AlgoMeas> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final long timeNs;
    private final int maxCptDelta;
    private final InterfaceAlgo algo;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public AlgoMeas(
        long timeNs,
        int maxCptDelta,
        InterfaceAlgo algo) {
        this.timeNs = timeNs;
        this.maxCptDelta = maxCptDelta;
        this.algo = algo;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return JisTestUtils.toStringTimeMeasureNs(this.timeNs)
            + ":"
            + this.algo
            + (this.maxCptDelta >= 0 ? "(" + this.maxCptDelta + ")" : "");
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) this.timeNs;
        result = prime * result + this.maxCptDelta;
        result = prime * result + this.algo.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null)
            || (obj.getClass() != this.getClass())) {
            return false;
        }
        final AlgoMeas other = (AlgoMeas) obj;
        return (this.timeNs == other.timeNs)
            && (this.maxCptDelta == other.maxCptDelta)
            && this.algo.equals(other.algo);
    }
    
    @Override
    public int compareTo(AlgoMeas other) {
        if (this == other) {
            return 0;
        }
        int cmp;
        /*
         * Sorted by increasing time,
         * and for same time (if ever happens) by increasing delta.
         */
        if (((cmp = Long.compare(this.timeNs, other.timeNs)) != 0)
            || ((cmp = Integer.compare(this.maxCptDelta, other.maxCptDelta)) != 0)
            || ((cmp = this.algo.compareTo(other.algo)) != 0)) {
            // empty
        }
        return cmp;
    }

    public long getTimeNs() {
        return this.timeNs;
    }

    public int getMaxCptDelta() {
        return this.maxCptDelta;
    }
    
    public InterfaceAlgo getAlgo() {
        return this.algo;
    }
}
