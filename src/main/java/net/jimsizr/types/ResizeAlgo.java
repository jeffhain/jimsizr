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
public final class ResizeAlgo implements InterfaceAlgo {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int ALGO_INDEX_BIT_SIZE =
        2 * InterKind.ordinalBitSize()
        + AlgoBrand.ordinalBitSize();
    
    private static final int ALGO_INDEX_BOUND = ((1 << ALGO_INDEX_BIT_SIZE) - 1);
    
    /**
     * Cached instances, to avoid garbage and memory usage.
     */
    private static final ResizeAlgo[] ALGO_BY_ALGO_INDEX =
        new ResizeAlgo[ALGO_INDEX_BOUND];
    
    private static final List<ResizeAlgo> VALUE_LIST;

    static {
        final List<ResizeAlgo> list = new ArrayList<ResizeAlgo>();
        
        for (InterKind srcIK : InterKind.valueList()) {
            for (AlgoBrand algoBrand : AlgoBrand.valueList()) {
                for (InterKind dstIK : InterKind.valueList()) {
                    final int algoIndex =
                        computeResizeAlgoIndex(
                            srcIK,
                            algoBrand,
                            dstIK);
                    final ResizeAlgo algo = new ResizeAlgo(srcIK, algoBrand, dstIK);
                    
                    // Index unicity check (built-in test).
                    final ResizeAlgo prev = ALGO_BY_ALGO_INDEX[algoIndex];
                    if (prev != null) {
                        throw new AssertionError("duplicate algo index: " + algoIndex);
                    }
                    
                    ALGO_BY_ALGO_INDEX[algoIndex] = algo;
                    list.add(algo);
                }
            }
        }
        
        VALUE_LIST = Collections.unmodifiableList(list);
    }
    
    /*
     * 
     */
    
    private final InterKind srcIK;
    private final AlgoBrand brand;
    private final InterKind dstIK;
    
    private final List<InterKind> interKindList;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public ResizeAlgo(
        InterKind srcIK,
        AlgoBrand brand,
        InterKind dstIK) {
        this.srcIK = JisUtils.requireNonNull(srcIK);
        this.brand = JisUtils.requireNonNull(brand);
        this.dstIK = JisUtils.requireNonNull(dstIK);
        
        final List<InterKind> ikList = new ArrayList<InterKind>();
        if (srcIK != InterKind.NONE) {
            ikList.add(srcIK);
        }
        if (dstIK != InterKind.NONE) {
            ikList.add(dstIK);
        }
        this.interKindList = Collections.unmodifiableList(ikList);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.srcIK != InterKind.NONE) {
            sb.append(this.srcIK);
            sb.append("_");
        }
        sb.append(this.brand);
        if (this.dstIK != InterKind.NONE) {
            sb.append("_");
            sb.append(this.dstIK);
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.srcIK.hashCode();
        result = prime * result + this.brand.hashCode();
        result = prime * result + this.dstIK.hashCode();
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
        final ResizeAlgo other = (ResizeAlgo) obj;
        return (this.srcIK == other.srcIK)
            && (this.brand == other.brand)
            && (this.dstIK == other.dstIK);
    }
    
    @Override
    public int compareTo(InterfaceAlgo other) {
        if (this == other) {
            return 0;
        }
        final ResizeAlgo o = (ResizeAlgo) other;
        int cmp;
        if (((cmp = this.srcIK.compareTo(o.srcIK)) != 0)
            || ((cmp = this.brand.compareTo(o.brand)) != 0)
            || ((cmp = this.dstIK.compareTo(o.dstIK)) != 0)) {
            // empty
        }
        return cmp;
    }
    
    /*
     * 
     */

    public InterKind getSrcIK() {
        return this.srcIK;
    }

    @Override
    public AlgoBrand getBrand() {
        return this.brand;
    }

    public InterKind getDstIK() {
        return this.dstIK;
    }
    
    @Override
    public boolean isRaw() {
        return (this.srcIK == InterKind.NONE)
            && (this.dstIK == InterKind.NONE);
    }
    
    @Override
    public List<InterKind> getInterKindList() {
        return this.interKindList;
    }
    
    /*
     * 
     */
    
    public static byte computeResizeAlgoIndex(
        InterKind srcIK,
        AlgoBrand algoBrand,
        InterKind dstIK) {
        int retInt = srcIK.ordinal();
        //
        retInt <<= AlgoBrand.ordinalBitSize();
        retInt += algoBrand.ordinal();
        //
        retInt <<= InterKind.ordinalBitSize();
        retInt += dstIK.ordinal();
        //
        final byte ret = (byte) retInt;
        if (ret != retInt) {
            throw new AssertionError("algo index not in byte range: " + retInt);
        }
        return ret;
    }
    
    /**
     * @param index [srcIK.ordinal(),algoBrand.ordinal(),dstIK.ordinal()] bits,
     *        as returned by computeAlgoIndex().
     * @return IllegalArgumentException if the index is
     *         in min/max range but invalid
     *         (not all indexes in range correspond to a value).
     */
    public static ResizeAlgo valueOfIndex(int index) {
        final ResizeAlgo ret = ALGO_BY_ALGO_INDEX[index];
        if (ret == null) {
            throw new IllegalArgumentException("invalid index: " + index);
        }
        return ret;
    }
    
    public static List<ResizeAlgo> valueList() {
        return VALUE_LIST;
    }
}
