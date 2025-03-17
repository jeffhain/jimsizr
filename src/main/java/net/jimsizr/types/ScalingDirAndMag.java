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

public enum ScalingDirAndMag {
    D8(false, ScalingMag.M8),
    D4(false, ScalingMag.M4),
    D2(false, ScalingMag.M2),
    D1(false, ScalingMag.M1),
    U1(true, ScalingMag.M1),
    U2(true, ScalingMag.M2),
    U4(true, ScalingMag.M4),
    U8(true, ScalingMag.M8);
    
    /**
     * Must be in ]0,1[.
     */
    private static final double SPAN_RATIO_THRESHOLD = 0.5;
    
    private static final int ORDINAL_BIT_SIZE =
        JisTypesInternals.unsignedBitSize(
            ScalingDirAndMag.values().length - 1);
    
    private final boolean upElseDown;
    private final ScalingMag mag;
    
    private ScalingDirAndMag(
        boolean upElseDown,
        ScalingMag mag) {
        this.upElseDown = upElseDown;
        this.mag = mag;
    }

    public boolean isUpElseDown() {
        return this.upElseDown;
    }

    public ScalingMag getMag() {
        return this.mag;
    }

    public static ScalingDirAndMag valueOf(
        boolean upElseDown,
        ScalingMag mag) {
        switch (mag) {
            case M8: return (upElseDown ? U8 : D8);
            case M4: return (upElseDown ? U4 : D4);
            case M2: return (upElseDown ? U2 : D2);
            case M1: return (upElseDown ? U1 : D1);
            default:
                throw new AssertionError();
        }
    }
    
    public static ScalingDirAndMag valueOfSpans(
        int srcWidth,
        int srcHeight,
        int dstWidth,
        int dstHeight) {
        
        /*
         * Fast computation: just one division, no Math call.
         */
        
        final boolean upElseDown;
        final double areaFactor;
        {
            final double srcArea = srcWidth * (double) srcHeight;
            final double dstArea = dstWidth * (double) dstHeight;
            // If equal, is down.
            upElseDown = (srcArea < dstArea);
            if (upElseDown) {
                areaFactor = dstArea / srcArea;
            } else {
                areaFactor = srcArea / dstArea;
            }
        }
        
        // In [4,8].
        final double spanFactorThresholdC8 = 4 + 4 * SPAN_RATIO_THRESHOLD;
        // In [16,64].
        final double areaFactorThresholdC8 = (spanFactorThresholdC8 * spanFactorThresholdC8);
        // Up enough to use choice for (2^3 * spans) upscaling.
        final boolean upEnoughC8 = (areaFactor > areaFactorThresholdC8);
        if (upEnoughC8) {
            return valueOf(upElseDown, ScalingMag.M8);
        }
        
        // In [2,4].
        final double spanFactorThresholdC4 = 2 + 2 * SPAN_RATIO_THRESHOLD;
        // In [4,16].
        final double areaFactorThresholdC4 = (spanFactorThresholdC4 * spanFactorThresholdC4);
        // Up enough to use choice for (2^2 * spans) upscaling.
        final boolean upEnoughC4 = (areaFactor > areaFactorThresholdC4);
        if (upEnoughC4) {
            return valueOf(upElseDown, ScalingMag.M4);
        }
        
        // In [1,2].
        final double spanFactorThresholdC2 = 1 + SPAN_RATIO_THRESHOLD;
        // In [1,4].
        final double areaFactorThresholdC2 = (spanFactorThresholdC2 * spanFactorThresholdC2);
        // Up enough to use choice for (2^1 * spans) upscaling.
        final boolean upEnoughC2 = (areaFactor > areaFactorThresholdC2);
        if (upEnoughC2) {
            return valueOf(upElseDown, ScalingMag.M2);
        }
        
        return valueOf(upElseDown, ScalingMag.M1);
    }
    
    public static int ordinalBitSize() {
        return ORDINAL_BIT_SIZE;
    }
}
