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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.jimsizr.test.tuning.types.AlgoMeas;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.CopyAlgo;
import net.jimsizr.types.CopyInterKinds;
import net.jimsizr.types.InterfaceAlgo;
import net.jimsizr.types.ResizeAlgo;
import net.jimsizr.types.ScalingDirAndMag;
import net.jimsizr.utils.BihTestUtils;

public class BestMeasComparatorTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BestMeasComparatorTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_contract_respected_CopyAlgo() {
        this.test_contract_respected_xxx(false);
    }
    
    public void test_contract_respected_ResizeAlgo() {
        this.test_contract_respected_xxx(true);
    }
    
    public void test_contract_respected_xxx(boolean resizeElseCopy) {
        final Random random = BihTestUtils.newRandom();
        
        // Many because with just 3 per check,
        // might be "lucky" a few times.
        final int checkCount = 10 * 1000;
        // Just need 3, easier to analyze when fails.
        final int measCountPerCheck = 3;
        
        final ScalingDirAndMag[] ScalingDirAndMag_VALUES =
            ScalingDirAndMag.values();
        
        for (int k = 0; k < checkCount; k++) {
            final ScalingDirAndMag scalingDirAndMag;
            if (resizeElseCopy) {
                final int sdamIndex =
                    random.nextInt(ScalingDirAndMag_VALUES.length);
                scalingDirAndMag =
                    ScalingDirAndMag_VALUES[sdamIndex];
            } else {
                scalingDirAndMag = null;
            }
            final BestMeasComparator comparator =
                new BestMeasComparator(
                    scalingDirAndMag);
            
            final List<AlgoMeas> measList =
                randomMeasList(
                    random,
                    resizeElseCopy,
                    measCountPerCheck);
            
            // Might throw if contract not respected.
            Collections.sort(measList, comparator);
            
            // Checking that contract respected.
            for (int i = 0; i < measCountPerCheck; i++) {
                final AlgoMeas m1 = measList.get(i);
                for (int j = i+1; j < measCountPerCheck; j++) {
                    final AlgoMeas m2 = measList.get(j);
                    if (comparator.compare(m1, m2) > 0) {
                        System.out.println("measList = " + measList);
                        System.out.println(m1 + " > " + m2);
                        fail("comparator is not transitive");
                    }
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static List<AlgoMeas> randomMeasList(
        Random random,
        boolean resizeElseCopy,
        int size){
        final List<AlgoMeas> measList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final AlgoMeas meas = randomMeas(random, resizeElseCopy);
            measList.add(meas);
        }
        return measList;
    }
    
    private static AlgoMeas randomMeas(Random random, boolean resizeElseCopy) {
        final long timeNs = random.nextLong() & ((long) 1e9);
        final int maxCptDelta = random.nextInt(10);
        final InterfaceAlgo algo = randomAlgo(random, resizeElseCopy);
        return new AlgoMeas(timeNs, maxCptDelta, algo);
    }
    
    private static InterfaceAlgo randomAlgo(Random random, boolean resizeElseCopy) {
        return resizeElseCopy ? randomResizeAlgo(random) : randomCopyAlgo(random);
    }
    
    private static CopyAlgo randomCopyAlgo(Random random) {
        final List<AlgoBrand> brandList = AlgoBrand.valueList();
        final List<CopyInterKinds> iksList = CopyInterKinds.valueList();
        return CopyAlgo.valueOf(
            iksList.get(random.nextInt(iksList.size())),
            brandList.get(random.nextInt(brandList.size())));
    }
    
    private static ResizeAlgo randomResizeAlgo(Random random) {
        final List<ResizeAlgo> algoList = ResizeAlgo.valueList();
        return algoList.get(random.nextInt(algoList.size()));
    }
}
