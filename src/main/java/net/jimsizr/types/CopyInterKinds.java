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

/**
 * Kinds of intermediary image to use before using raw copy algo.
 * 
 * Can have up to 3, which might be required for best speed and accuracy
 * when destination is TYPE_BYTE_BINARY.
 */
public enum CopyInterKinds {
    NONE(),
    //
    CO(InterKind.CO),
    CO_BW(InterKind.CO, InterKind.BW),
    CO_BW_CO(InterKind.CO, InterKind.BW, InterKind.CO),
    CO_BW_ACO(InterKind.CO, InterKind.BW, InterKind.ACO),
    CO_BW_ACOP(InterKind.CO, InterKind.BW, InterKind.ACOP),
    //
    ACO(InterKind.ACO),
    ACO_BW(InterKind.ACO, InterKind.BW),
    ACO_BW_CO(InterKind.ACO, InterKind.BW, InterKind.CO),
    ACO_BW_ACO(InterKind.ACO, InterKind.BW, InterKind.ACO),
    ACO_BW_ACOP(InterKind.ACO, InterKind.BW, InterKind.ACOP),
    //
    ACOP(InterKind.ACOP),
    ACOP_BW(InterKind.ACOP, InterKind.BW),
    ACOP_BW_CO(InterKind.ACOP, InterKind.BW, InterKind.CO),
    ACOP_BW_ACO(InterKind.ACOP, InterKind.BW, InterKind.ACO),
    ACOP_BW_ACOP(InterKind.ACOP, InterKind.BW, InterKind.ACOP),
    //
    BW(InterKind.BW),
    BW_CO(InterKind.BW, InterKind.CO),
    BW_ACO(InterKind.BW, InterKind.ACO),
    BW_ACOP(InterKind.BW, InterKind.ACOP);
    
    private static final List<CopyInterKinds> VALUE_LIST =
        Collections.unmodifiableList(Arrays.asList(CopyInterKinds.values()));
    
    private final List<InterKind> interKindList;
    
    private CopyInterKinds(InterKind... interKinds) {
        this.interKindList = Collections.unmodifiableList(Arrays.asList(interKinds));
    }
    
    public List<InterKind> interKindList() {
        return this.interKindList;
    }
    
    public int interKindCount() {
        return this.interKindList.size();
    }
    
    /**
     * @return null if none
     */
    public InterKind firstInterKind() {
        final int n = this.interKindList.size();
        return ((n != 0) ? this.interKindList.get(0) : null);
    }
    
    /**
     * @return null if none
     */
    public InterKind lastInterKind() {
        final int n = this.interKindList.size();
        return ((n != 0) ? this.interKindList.get(n - 1) : null);
    }
    
    public boolean containsOpaque() {
        boolean ret = false;
        for (InterKind interKind : this.interKindList) {
            if (interKind.isOpaque()) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    
    public boolean containsBlackAndWhite() {
        boolean ret = false;
        for (InterKind interKind : this.interKindList) {
            if (interKind.isBlackAndWhite()) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    
    public static List<CopyInterKinds> valueList() {
        return VALUE_LIST;
    }
}
