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

import java.util.List;

public interface InterfaceAlgo extends Comparable<InterfaceAlgo> {
    
    /**
     * @return Algo brand.
     */
    public AlgoBrand getBrand();
    
    /**
     * @return True if uses no intermediary image type before or after
     *         brand's raw algo (which itself might still
     *         use intermediary image types internally).
     */
    public boolean isRaw();
    
    /**
     * @return The intermediary image types (not NONE) used
     *         in addition to brand's raw algo.
     */
    public List<InterKind> getInterKindList();
}
