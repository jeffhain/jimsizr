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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum AlgoBrand {
    /**
     * Graphics.drawImage().
     */
    AWT,
    /**
     * Java implementations by this library
     * (possibly with delegation to drawImage()
     * from BufferedImageHelper which tries to be fast).
     */
    JIS;
    
    private static final AlgoBrand[] VALUES = AlgoBrand.values();
    private static final int ORDINAL_BIT_SIZE = JisTypesInternals.unsignedBitSize(VALUES.length - 1);
    private static final List<AlgoBrand> VALUE_LIST =
        Collections.unmodifiableList(Arrays.asList(VALUES));
    
    public static List<AlgoBrand> valueList() {
        return VALUE_LIST;
    }
    
    public static int ordinalBitSize() {
        return ORDINAL_BIT_SIZE;
    }
}
