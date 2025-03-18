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
package net.jimsizr.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jimsizr.utils.JisUtils;

/**
 * Immutable.
 */
public final class CopyAlgo implements InterfaceAlgo {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int ALGO_INDEX_BIT_SIZE =
        3 * InterKind.ordinalBitSize()
        + AlgoBrand.ordinalBitSize();
    
    private static final int ALGO_INDEX_BOUND = ((1 << ALGO_INDEX_BIT_SIZE) - 1);
    
    /**
     * Cached instances, to avoid garbage and memory usage.
     */
    private static final CopyAlgo[] ALGO_BY_ALGO_INDEX =
        new CopyAlgo[ALGO_INDEX_BOUND];
    
    private static final List<CopyAlgo> VALUE_LIST;

    static {
        final List<CopyAlgo> list = new ArrayList<CopyAlgo>();
        
        for (AlgoBrand algoBrand : AlgoBrand.valueList()) {
            for (CopyInterKinds interKinds : CopyInterKinds.valueList()) {
                final int algoImplIndex =
                    computeCopyAlgoIndex(
                        interKinds,
                        algoBrand);
                final CopyAlgo algo = new CopyAlgo(interKinds, algoBrand);
                ALGO_BY_ALGO_INDEX[algoImplIndex] = algo;
                list.add(algo);
            }
        }
        
        VALUE_LIST = Collections.unmodifiableList(list);
    }
    
    /*
     * 
     */
    
    private final CopyInterKinds interKinds;
    private final AlgoBrand brand;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private CopyAlgo(
        CopyInterKinds interKinds,
        AlgoBrand brand) {
        this.interKinds = JisUtils.requireNonNull(interKinds);
        this.brand = JisUtils.requireNonNull(brand);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.interKinds != CopyInterKinds.NONE) {
            sb.append(this.interKinds);
            sb.append("_");
        }
        sb.append(this.brand);
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.interKinds.hashCode();
        result = prime * result + this.brand.hashCode();
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
        final CopyAlgo other = (CopyAlgo) obj;
        return (this.interKinds == other.interKinds)
            && (this.brand == other.brand);
    }
    
    @Override
    public int compareTo(InterfaceAlgo other) {
        if (this == other) {
            return 0;
        }
        final CopyAlgo o = (CopyAlgo) other;
        int cmp;
        if (((cmp = this.interKinds.compareTo(o.interKinds)) != 0)
            || ((cmp = this.brand.compareTo(o.brand)) != 0)) {
            // empty
        }
        return cmp;
    }
    
    /*
     * 
     */

    public CopyInterKinds getInterKinds() {
        return this.interKinds;
    }

    @Override
    public AlgoBrand getBrand() {
        return this.brand;
    }
    
    @Override
    public boolean isRaw() {
        return (this.interKinds == CopyInterKinds.NONE);
    }
    
    @Override
    public List<InterKind> getInterKindList() {
        return this.interKinds.interKindList();
    }
    
    /*
     * 
     */
    
    public static byte computeCopyAlgoIndex(
        CopyInterKinds interKinds,
        AlgoBrand algoBrand) {
        int retInt = interKinds.ordinal();
        //
        retInt <<= AlgoBrand.ordinalBitSize();
        retInt += algoBrand.ordinal();
        //
        final byte ret = (byte) retInt;
        if (ret != retInt) {
            throw new AssertionError("algo index not in byte range: " + retInt);
        }
        return ret;
    }
    
    /**
     * @param index [interKinds.ordinal(),algoBrand.ordinal()] bits,
     *        as returned by computeAlgoIndex().
     * @return IllegalArgumentException if the index is
     *         in min/max range but invalid
     *         (not all indexes in range correspond to a value).
     */
    public static CopyAlgo valueOfIndex(int index) {
        final CopyAlgo ret = ALGO_BY_ALGO_INDEX[index];
        if (ret == null) {
            throw new IllegalArgumentException("invalid index: " + index);
        }
        return ret;
    }
    
    public static CopyAlgo valueOf(CopyInterKinds interKinds, AlgoBrand algoBrand) {
        final int index = computeCopyAlgoIndex(interKinds, algoBrand);
        return ALGO_BY_ALGO_INDEX[index];
    }
    
    public static List<CopyAlgo> valueList() {
        return VALUE_LIST;
    }
}
