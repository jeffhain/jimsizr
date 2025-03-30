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
import java.util.SortedSet;
import java.util.TreeSet;

public class ExtendedImageTypes {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Using same order as standard types int's order.
     */
    private static final SortedSet<Integer> STANDARD_IMAGE_TYPE_SET =
        new TreeSet<Integer>(Arrays.asList(
            BufferedImage.TYPE_CUSTOM,
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_INT_ARGB_PRE,
            BufferedImage.TYPE_INT_BGR,
            BufferedImage.TYPE_3BYTE_BGR,
            BufferedImage.TYPE_4BYTE_ABGR,
            BufferedImage.TYPE_4BYTE_ABGR_PRE,
            BufferedImage.TYPE_USHORT_565_RGB,
            BufferedImage.TYPE_USHORT_555_RGB,
            BufferedImage.TYPE_BYTE_GRAY,
            BufferedImage.TYPE_USHORT_GRAY,
            BufferedImage.TYPE_BYTE_BINARY,
            BufferedImage.TYPE_BYTE_INDEXED));
    
    /**
     * We define this type to cover TYPE_CUSTOM types that are alpha-premultiplied,
     * without having to use additional srcPremul/dstPremul booleans,
     * that would only be useful in case of TYPE_CUSTOM.
     * As a result, we will only use TYPE_CUSTOM for non-alpha-premultiplied cases.
     */
    private static final int TYPE_CUSTOM_PRE = STANDARD_IMAGE_TYPE_SET.last() + 1;
    
    public static final SortedSet<Integer> EXTENDED_IMAGE_TYPE_SET;
    static {
        final TreeSet<Integer> set = new TreeSet<Integer>(STANDARD_IMAGE_TYPE_SET);
        set.add(TYPE_CUSTOM_PRE);
        EXTENDED_IMAGE_TYPE_SET = Collections.unmodifiableSortedSet(set);
    }
    
    /**
     * Allowing for negative image types (why not).
     */
    private static final int MIN_EXTENDED_IMAGE_TYPE = EXTENDED_IMAGE_TYPE_SET.first();
    private static final int MAX_EXTENDED_IMAGE_TYPE = EXTENDED_IMAGE_TYPE_SET.last();
    private static final int EXTENDED_IMAGE_TYPE_SPAN =
        (MAX_EXTENDED_IMAGE_TYPE - MIN_EXTENDED_IMAGE_TYPE + 1);
    
    public static final int EXTENDED_IMAGE_TYPE_INDEX_BIT_SIZE =
        JisTypesInternals.unsignedBitSize(EXTENDED_IMAGE_TYPE_SPAN - 1);
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private ExtendedImageTypes() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static int toExtendedImageTypeIndex(int extendedImageType) {
        return extendedImageType - MIN_EXTENDED_IMAGE_TYPE;
    }
    
    public static int toExtendedImageType(int imageType, boolean premul) {
        if (!STANDARD_IMAGE_TYPE_SET.contains(imageType)) {
            imageType = BufferedImage.TYPE_CUSTOM;
        }
        if ((imageType == BufferedImage.TYPE_CUSTOM) && premul) {
            imageType = TYPE_CUSTOM_PRE;
        }
        return imageType;
    }
}
