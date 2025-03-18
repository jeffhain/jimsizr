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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Kind of intermediary image used to make algos faster
 * and/or more accurate.
 * 
 * CO = COLORED (TYPE_INT_ARGB or TYPE_INT_ARGB_PRE)
 *      Good because versatile and drawImage() optimized for it.
 * BW = BLACK AND WHITE (TYPE_BYTE_GRAY)
 *      Good because fast and, unlike IC, preserves proper
 *      intensity when destination is TYPE_BYTE_BINARY,
 *      and TYPE_USHORT_GRAY and TYPE_BYTE_BINARY
 *      might be very slow when parallelized.
 */
public enum InterKind {
    NONE(-1, 0),
    /**
     * COlored, with Alpha, and alpha-Premultiplied.
     * 
     * Corresponds to TYPE_INT_ARGB_PRE, with which drawImage()
     * is typically fast and accurate.
     */
    ACOP(BufferedImage.TYPE_INT_ARGB_PRE, 4),
    /**
     * COlored, with Alpha, not alpha-premultiplied.
     * 
     * Corresponds to TYPE_INT_ARGB, with which drawImage()
     * is typically fast and accurate.
     */
    ACO(BufferedImage.TYPE_INT_ARGB, 4),
    /**
     * COlored, without alpha.
     * 
     * Corresponds to TYPE_INT_RGB, with which drawImage()
     * is typically fast and accurate.
     */
    CO(BufferedImage.TYPE_INT_RGB, 4),
    /**
     * Black and White.
     * 
     * Corresponds to TYPE_BYTE_GRAY, with which drawImage()
     * is typically fast and accurate.
     * 
     * If using TYPE_USHORT_GRAY instead, as destination type,
     * drawImage() is terribly slow in case of parallelism
     * (which also happens with TYPE_BYTE_BINARY).
     * 
     * Unlike COlored flavors, allows to preserve proper
     * intensity when destination is TYPE_BYTE_BINARY.
     */
    BW(BufferedImage.TYPE_BYTE_GRAY, 1);
    
    private static final InterKind[] VALUES = InterKind.values();
    private static final int ORDINAL_BIT_SIZE = JisTypesInternals.unsignedBitSize(VALUES.length - 1);
    private static final List<InterKind> VALUE_LIST =
        Collections.unmodifiableList(Arrays.asList(VALUES));
    
    private final int imageType;
    
    private final int pixelByteSize;
    
    private InterKind(
        int imageType,
        int pixelByteSize) {
        this.imageType = imageType;
        this.pixelByteSize = pixelByteSize;
    }
    
    public int getImageType() {
        return this.imageType;
    }
    
    public int getPixelByteSize() {
        return this.pixelByteSize;
    }
    
    public boolean isOpaque() {
        return (this == CO)
            || (this == BW);
    }
    
    public boolean isBlackAndWhite() {
        return (this == BW);
    }
    
    public static List<InterKind> valueList() {
        return VALUE_LIST;
    }
    
    public static int ordinalBitSize() {
        return ORDINAL_BIT_SIZE;
    }
}
