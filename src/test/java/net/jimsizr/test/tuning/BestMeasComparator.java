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

import java.util.Comparator;

import net.jimsizr.test.tuning.types.AlgoMeas;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.InterKind;
import net.jimsizr.types.InterfaceAlgo;
import net.jimsizr.types.ResizeAlgo;
import net.jimsizr.types.ScalingDirAndMag;

/**
 * To be used only on measures which are all
 * accurate and fast enough to have been retained.
 */
public class BestMeasComparator implements Comparator<AlgoMeas> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final ScalingDirAndMag resizeDirAndMag;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * @param resizeDirAndMag Null for copy.
     */
    public BestMeasComparator(ScalingDirAndMag resizeDirAndMag) {
        this.resizeDirAndMag = resizeDirAndMag;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public int compare(AlgoMeas m1, AlgoMeas m2) {
        final InterfaceAlgo a1 = m1.getAlgo();
        final InterfaceAlgo a2 = m2.getAlgo();
        /*
         * 1) The most accurate (might be luck, might not).
         */
        {
            final int cmp = m1.getMaxCptDelta() - m2.getMaxCptDelta();
            if (cmp != 0) {
                return cmp;
            }
        }
        /*
         * 2) AWT/other: AWT, which is the most reliable
         *    (C code when fast, so no JIT surprise.
         *    Switching to JIS only when "clearly" better).
         */
        {
            final boolean is1Awt = (a1.getBrand() == AlgoBrand.AWT);
            final boolean is2Awt = (a2.getBrand() == AlgoBrand.AWT);
            if (is1Awt != is2Awt) {
                return (is1Awt ? -1 : 1);
            }
        }
        /*
         * 3) The one which intermediary images take the least bytes
         *    (helping allocation not be a bottleneck).
         */
        if (a1 instanceof ResizeAlgo) {
            final int cmp = this.compareInterKindsMemoryForResize(
                (ResizeAlgo) a1,
                (ResizeAlgo) a2);
            if (cmp != 0) {
                return cmp;
            }
        } else {
            final int cmp = compareInterKindsMemoryForCopy(a1, a2);
            if (cmp != 0) {
                return cmp;
            }
        }
        /*
         * 4) The fastest.
         */
        {
            final long t1 = m1.getTimeNs();
            final long t2 = m2.getTimeNs();
            if (t1 != t2) {
                return (t1 < t2) ? -1 : 1;
            }
        }
        /*
         * 5) Last ressort: algo comparator.
         */
        return a1.compareTo(a2);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private int compareInterKindsMemoryForResize(
        ResizeAlgo r1,
        ResizeAlgo r2) {
        final int mag = this.resizeDirAndMag.getMag().getSpanRefScalingMag();
        // Area factor for the largest side.
        final int bigSideAreaFactor;
        if (mag == 1) {
            bigSideAreaFactor = 2;
        } else {
            bigSideAreaFactor = mag * mag;
        }
        final int srcAreaFactor;
        final int dstAreaFactor;
        if (this.resizeDirAndMag.isUpElseDown()) {
            srcAreaFactor = 1;
            dstAreaFactor = bigSideAreaFactor;
        } else {
            srcAreaFactor = bigSideAreaFactor;
            dstAreaFactor = 1;
        }
        final int areaCost1 =
            srcAreaFactor * r1.getSrcIK().getPixelByteSize()
            + dstAreaFactor * r1.getDstIK().getPixelByteSize();
        final int areaCost2 =
            srcAreaFactor * r2.getSrcIK().getPixelByteSize()
            + dstAreaFactor * r2.getDstIK().getPixelByteSize();
        return areaCost1 - areaCost2;
    }
    
    private static int compareInterKindsMemoryForCopy(
        InterfaceAlgo a1,
        InterfaceAlgo a2) {
        int areaCost1 = 0;
        for (InterKind ik : a1.getInterKindList()) {
            areaCost1 += ik.getPixelByteSize();
        }
        int areaCost2 = 0;
        for (InterKind ik : a2.getInterKindList()) {
            areaCost2 += ik.getPixelByteSize();
        }
        return areaCost1 - areaCost2;
    }
}
