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
package net.jimsizr.test.utils;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jimsizr.utils.BufferedImageHelper.BihPixelFormat;

/**
 * BufferedImage image types for test,
 * as an enum, plus various custom types:
 * - all standard image types,
 * - all BihPixelFormat values (being complete is useful for benches,
 *   also it simplifies test images creation),
 * - and more (GRB order), but always with alpha,
 *   to keep things simple and simply versatile.
 * 
 * Using same order as ExtendedImageTypes.EXTENDED_IMAGE_TYPE_SET,
 * which also respects standard types int's order
 * (except for TYPE_CUSTOM_PRE being last).
 * Also allows for use case indexes to increase
 * when iterating over image types.
 */
public enum TestImageTypeEnum {
    TYPE_CUSTOM_INT_ABGR(BihPixelFormat.ABGR32, false),
    TYPE_CUSTOM_INT_RGBA(BihPixelFormat.RGBA32, false),
    TYPE_CUSTOM_INT_BGRA(BihPixelFormat.BGRA32, false),
    TYPE_CUSTOM_INT_GRAB(false),
    //
    TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB),
    TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB),
    TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE),
    //
    TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR),
    //
    TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR),
    TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR),
    TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE),
    //
    TYPE_USHORT_565_RGB(BufferedImage.TYPE_USHORT_565_RGB),
    TYPE_USHORT_555_RGB(BufferedImage.TYPE_USHORT_555_RGB),
    //
    TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY),
    TYPE_USHORT_GRAY(BufferedImage.TYPE_USHORT_GRAY),
    //
    TYPE_BYTE_BINARY(BufferedImage.TYPE_BYTE_BINARY),
    TYPE_BYTE_INDEXED(BufferedImage.TYPE_BYTE_INDEXED),
    //
    TYPE_CUSTOM_INT_ABGR_PRE(BihPixelFormat.ABGR32, true),
    TYPE_CUSTOM_INT_RGBA_PRE(BihPixelFormat.RGBA32, true),
    TYPE_CUSTOM_INT_BGRA_PRE(BihPixelFormat.BGRA32, true),
    TYPE_CUSTOM_INT_GRAB_PRE(true);
    /*
     * 
     */
    private final int imageType;
    private final BihPixelFormat pixelFormat;
    private final boolean premul;
    private TestImageTypeEnum(int imageType) {
        this.imageType = imageType;
        this.pixelFormat = BihPixelFormat.fromImageType(imageType);
        this.premul =
            (imageType == BufferedImage.TYPE_INT_ARGB_PRE)
            || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE);
    }
    private TestImageTypeEnum(BihPixelFormat pixelFormat, boolean premul) {
        // Implicit null check.
        // Does premul consistency check.
        this.imageType = pixelFormat.toImageType(premul);
        this.pixelFormat = pixelFormat;
        this.premul = premul;
    }
    /**
     * For TYPE_CUSTOM without corresponding BihPixelFormat value.
     */
    private TestImageTypeEnum(boolean premul) {
        this.imageType = BufferedImage.TYPE_CUSTOM;
        this.pixelFormat = null;
        this.premul = premul;
    }
    /**
     * @return Null if none, or if imageType is TYPE_CUSTOM (multiple results possible).
     */
    public static TestImageTypeEnum valueOfImageType(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_INT_ARGB_PRE: return TYPE_INT_ARGB_PRE;
            case BufferedImage.TYPE_INT_ARGB: return TYPE_INT_ARGB;
            case BufferedImage.TYPE_INT_RGB: return TYPE_INT_RGB;
            //
            case BufferedImage.TYPE_INT_BGR: return TYPE_INT_BGR;
            //
            case BufferedImage.TYPE_4BYTE_ABGR_PRE: return TYPE_4BYTE_ABGR_PRE;
            case BufferedImage.TYPE_4BYTE_ABGR: return TYPE_4BYTE_ABGR;
            case BufferedImage.TYPE_3BYTE_BGR: return TYPE_3BYTE_BGR;
            //
            case BufferedImage.TYPE_USHORT_565_RGB: return TYPE_USHORT_565_RGB;
            case BufferedImage.TYPE_USHORT_555_RGB: return TYPE_USHORT_555_RGB;
            case BufferedImage.TYPE_USHORT_GRAY: return TYPE_USHORT_GRAY;
            //
            case BufferedImage.TYPE_BYTE_GRAY: return TYPE_BYTE_GRAY;
            case BufferedImage.TYPE_BYTE_BINARY: return TYPE_BYTE_BINARY;
            case BufferedImage.TYPE_BYTE_INDEXED: return TYPE_BYTE_INDEXED;
            default:
                return null;
        }
    }
    /**
     * @return Null if none.
     */
    public static TestImageTypeEnum valueOfPixelFormat(BihPixelFormat pixelFormat, boolean premul) {
        switch (pixelFormat) {
            case ARGB32 : return (premul ? TYPE_INT_ARGB_PRE : TYPE_INT_ARGB);
            case ABGR32 : return (premul ? TYPE_CUSTOM_INT_ABGR_PRE : TYPE_CUSTOM_INT_ABGR);
            case RGBA32 : return (premul ? TYPE_CUSTOM_INT_RGBA_PRE : TYPE_CUSTOM_INT_RGBA);
            case BGRA32 : return (premul ? TYPE_CUSTOM_INT_BGRA_PRE : TYPE_CUSTOM_INT_BGRA);
            case XRGB24 : return (premul ? ite_throwIAE() : TYPE_INT_RGB);
            case XBGR24 : return (premul ? ite_throwIAE() : TYPE_INT_BGR);
            default:
                throw new IllegalArgumentException("" + pixelFormat);
        }
    }
    private static TestImageTypeEnum ite_throwIAE() {
        throw new IllegalArgumentException();
    }
    public String toStringShort() {
        switch (this) {
            case TYPE_INT_ARGB_PRE: return "ARGB_P";
            case TYPE_INT_ARGB: return "ARGB";
            case TYPE_INT_RGB: return "RGB";
            case TYPE_INT_BGR: return "BGR";
            case TYPE_4BYTE_ABGR_PRE: return "B_ABGR_P";
            case TYPE_4BYTE_ABGR: return "B_ABGR";
            case TYPE_3BYTE_BGR: return "B_BGR";
            case TYPE_USHORT_565_RGB: return "S_6_RGB";
            case TYPE_USHORT_555_RGB: return "S_5_RGB";
            case TYPE_USHORT_GRAY: return "S_GRAY";
            case TYPE_BYTE_GRAY: return "B_GRAY";
            case TYPE_BYTE_INDEXED: return "B_IND";
            case TYPE_BYTE_BINARY: return "B_BIN";
            default: return this.premul ? "CUSTOM_P" : "CUSTOM";
        }
    }
    public int imageType() {
        return this.imageType;
    }
    /**
     * Can be null.
     */
    public BihPixelFormat pixelFormat() {
        return this.pixelFormat;
    }
    public boolean isPremul() {
        return this.premul;
    }
    public boolean hasAlpha() {
        return (this == TYPE_INT_ARGB_PRE)
            || (this == TYPE_INT_ARGB)
            || (this == TYPE_4BYTE_ABGR_PRE)
            || (this == TYPE_4BYTE_ABGR)
            || ((this.pixelFormat != null)
                && this.pixelFormat.hasAlpha())
            || (this == TYPE_CUSTOM_INT_GRAB_PRE)
            || (this == TYPE_CUSTOM_INT_GRAB);
    }
    /**
     * @return True if is custom (premul or not).
     */
    public boolean isCustom() {
        return this.imageType == BufferedImage.TYPE_CUSTOM;
    }
    public boolean isWithAccurateColor() {
        return this.hasAlpha()
            || (this == TYPE_INT_RGB)
            || (this == TYPE_INT_BGR)
            || (this == TYPE_3BYTE_BGR);
    }
    public boolean isWithColor() {
        return !this.isBlackAndWhite();
    }
    public boolean isUShortRgb() {
        return (this == TYPE_USHORT_555_RGB)
            || (this == TYPE_USHORT_565_RGB);
    }
    public boolean isGray() {
        return (this == TYPE_USHORT_GRAY)
            || (this == TYPE_BYTE_GRAY);
    }
    public boolean isBlackAndWhite() {
        return this.isGray()
            || this.isBinary();
    }
    public boolean isBinary() {
        return (this == TYPE_BYTE_BINARY);
    }
    public boolean isIndexed() {
        return (this == TYPE_BYTE_INDEXED);
    }
    /*
     * 
     */
    private static final SortedMap<Integer,TestImageTypeEnum> ENUM_BY_TYPE;
    static {
        final SortedMap<Integer,TestImageTypeEnum> map = new TreeMap<>();
        for (TestImageTypeEnum ite : TestImageTypeEnum.values()) {
            map.put(ite.imageType(), ite);
        }
        ENUM_BY_TYPE = Collections.unmodifiableSortedMap(map);
    }
    public static SortedMap<Integer,TestImageTypeEnum> enumByType() {
        return ENUM_BY_TYPE;
    }
}
