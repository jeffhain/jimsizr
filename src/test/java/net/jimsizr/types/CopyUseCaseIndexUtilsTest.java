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

import java.util.TreeSet;

import junit.framework.TestCase;

public class CopyUseCaseIndexUtilsTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public CopyUseCaseIndexUtilsTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_computeCopyUseCaseIndex_unicity() {
        
        final TreeSet<Integer> indexSet = new TreeSet<>();
        
        for (int srcExtendedImageType : ExtendedImageTypes.EXTENDED_IMAGE_TYPE_SET) {
            for (int dstExtendedImageType : ExtendedImageTypes.EXTENDED_IMAGE_TYPE_SET) {
                final int index =
                    CopyUseCaseIndexUtils.computeCopyUseCaseIndex(
                        srcExtendedImageType,
                        dstExtendedImageType);
                final boolean didAdd = indexSet.add(index);
                if (!didAdd) {
                    throw new AssertionError();
                }
            }
        }
    }
}
