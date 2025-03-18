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
package net.jimsizr.scalers.basic.jis;

import net.jimsizr.utils.Argb32;
import net.jimsizr.utils.JisColorUtils;

/**
 * Weighted sum of pixel components.
 * Works with ARGB32 pixels.
 */
class PixelAccumulator {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Interpolating in premul, else RGB from low alpha pixels
     * would have same weight as RGB from high alpha pixels.
     */
    
    private double premulContribSumA8 = 0.0;
    private double premulContribSumR8 = 0.0;
    private double premulContribSumG8 = 0.0;
    private double premulContribSumB8 = 0.0;
    
    /*
     * Cache of components by pixel.
     * 
     * Never clearing this cache
     * (no need to, can only help).
     */
    
    private int newestPremulArgb32 = 0;
    
    private double newestPremulA8 = 0.0;
    private double newestPremulR8 = 0.0;
    private double newestPremulG8 = 0.0;
    private double newestPremulB8 = 0.0;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * Must be configured before use.
     */
    public PixelAccumulator() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Clears the accumulated weighted sum of components.
     */
    public void clearSum() {
        this.premulContribSumA8 = 0.0;
        this.premulContribSumR8 = 0.0;
        this.premulContribSumG8 = 0.0;
        this.premulContribSumB8 = 0.0;
    }
    
    /**
     * Uses a weight of 1.
     */
    public void addFullPixelContrib(int toAddPremulArgb32) {
        if (toAddPremulArgb32 != this.newestPremulArgb32) {
            this.updateNewestColorData(toAddPremulArgb32);
        }
        this.premulContribSumA8 += this.newestPremulA8;
        this.premulContribSumR8 += this.newestPremulR8;
        this.premulContribSumG8 += this.newestPremulG8;
        this.premulContribSumB8 += this.newestPremulB8;
    }
    
    /**
     * @param weight Pixel weight. Might be out of [0,1] (ex.: bicubic).
     */
    public void addPixelContrib(int toAddPremulArgb32, double weight) {
        if (toAddPremulArgb32 != this.newestPremulArgb32) {
            this.updateNewestColorData(toAddPremulArgb32);
        }
        this.premulContribSumA8 += weight * this.newestPremulA8;
        this.premulContribSumR8 += weight * this.newestPremulR8;
        this.premulContribSumG8 += weight * this.newestPremulG8;
        this.premulContribSumB8 += weight * this.newestPremulB8;
    }
    
    /**
     * Does not use saturation:
     * must only be used when each the sum of weighted components
     * divided by the sum of weights rounds into [0,0xFF].
     * 
     * @return The resulting alpha-premultiplied ARGB32.
     */
    public int toPremulArgb32(double dstPixelSurfInSrcInv) {
        final double H = 0.5;
        final int a8 = (int) (this.premulContribSumA8 * dstPixelSurfInSrcInv + H);
        final int r8 = (int) (this.premulContribSumR8 * dstPixelSurfInSrcInv + H);
        final int g8 = (int) (this.premulContribSumG8 * dstPixelSurfInSrcInv + H);
        final int b8 = (int) (this.premulContribSumB8 * dstPixelSurfInSrcInv + H);
        return JisColorUtils.toAbcd32_noCheck(a8, r8, g8, b8);
    }
    
    /**
     * Uses saturation to keep resulting components in [0,0xFF].
     * 
     * Implicitly uses 1 for absent dstPixelSurfInSrcInv argument:
     * considers that each sum of weighted components
     * corresponds to the surface of a single destination pixel.
     * 
     * @return The resulting alpha-premultiplied ARGB32.
     */
    public int toValidPremulArgb32UnitDstSurf() {
        final double H = 0.5;
        final int a8 = (int) (this.premulContribSumA8 + H);
        final int r8 = (int) (this.premulContribSumR8 + H);
        final int g8 = (int) (this.premulContribSumG8 + H);
        final int b8 = (int) (this.premulContribSumB8 + H);
        return JisColorUtils.toValidPremulAxyz32(a8, r8, g8, b8);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void updateNewestColorData(int toAddPremulColor32) {
        this.newestPremulA8 = Argb32.getAlpha8(toAddPremulColor32);
        this.newestPremulR8 = Argb32.getRed8(toAddPremulColor32);
        this.newestPremulG8 = Argb32.getGreen8(toAddPremulColor32);
        this.newestPremulB8 = Argb32.getBlue8(toAddPremulColor32);
        this.newestPremulArgb32 = toAddPremulColor32;
    }
}
