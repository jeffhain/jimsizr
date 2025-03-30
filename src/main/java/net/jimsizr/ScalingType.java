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
package net.jimsizr;

/**
 * Types of interpolation for image scaling.
 */
public enum ScalingType {
    /**
     * The fastest but typically less qualitative scaling type.
     * 
     * For large upscaling though, it preserves pixelation
     * somewhat better than BOXSAMPLED, since it does not interpolate,
     * and is much faster.
     */
    NEAREST("NEAR"),
    /**
     * Produces smooth but a bit blurry upscaling.
     * 
     * Does not take all source pixels into account
     * if downscaling divides spans by more than 2,
     * so you might want to use and iterative type
     * or BOXSAMPLED instead.
     */
    BILINEAR("BILI"),
    /**
     * Produces smooth upscaling,
     * less blurry than with BILINEAR,
     * and with sharpened edges.
     * 
     * Does not take all source pixels into account
     * if downscaling divides spans by more than 2,
     * so you might want to use and iterative type
     * or BOXSAMPLED instead.
     */
    BICUBIC("BICU"),
    /**
     * Produces accurate downscaling,
     * and upscaling that preserves pixelation,
     * which might be desired for example for pixel art.
     * 
     * Since AWT does not implement this scaling type,
     * we always use our Java implementation for it,
     * which is as accurate as possible (modulo in case
     * of transparency, due to the usual alpha-premultiplication
     * optimization), and gives the exact same result whether or not
     * it is parallelized, so this is the best choice for quality
     * and deterministic downscaling.
     */
    BOXSAMPLED("BOX"),
    /**
     * Iterates for downscaling to never divide span by more than two per step,
     * but does not iterate for upscaling.
     */
    ITERATIVE_BILINEAR("ITER_BILI"),
    /**
     * Iterates for downscaling to never divide span by more than two per step,
     * but does not iterate for upscaling.
     */
    ITERATIVE_BICUBIC("ITER_BICU");
    
    private final String shortString;
    
    private ScalingType(String shortString) {
        this.shortString = shortString;
    }
    
    /**
     * Useful for shorter logs.
     * 
     * @return A string usually shorter than name().
     */
    public String toStringShort() {
        return this.shortString;
    }
}
