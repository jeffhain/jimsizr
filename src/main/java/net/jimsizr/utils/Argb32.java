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
package net.jimsizr.utils;

/**
 * Utilities to deal with ARGB32 colors, as a primitive int.
 */
public class Argb32 {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------

    private Argb32() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Actually works for any components order,
     * since it just returns the hexadecimal string
     * corresponding to the four bytes.
     * 
     * @param argb32 A 32 bits ARGB color.
     * @return A string of the form 0xAARRGGBB.
     */
    public static String toString(int argb32) {
        final String intStr = Integer.toHexString(argb32).toUpperCase();
        final StringBuilder sb = new StringBuilder(10);
        sb.append("0x");
        for (int i = intStr.length(); i < 8; i++) {
            sb.append('0');
        }
        sb.append(intStr);
        return sb.toString();
    }
    
    /*
     * 
     */
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return True if the specified color is opaque,
     *         i.e. if its alpha component is 0xFF.
     */
    public static boolean isOpaque(int argb32) {
        final int alpha8 = getAlpha8(argb32);
        return (alpha8 == 0xFF);
    }
    
    /*
     * 
     */

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The alpha component in [0,255].
     */
    public static int getAlpha8(int argb32) {
        return (argb32 >> 24) & 0xFF;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The red component in [0,255].
     */
    public static int getRed8(int argb32) {
        return (argb32 >> 16) & 0xFF;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The green component in [0,255].
     */
    public static int getGreen8(int argb32) {
        return (argb32 >> 8) & 0xFF;
    }

    /**
     * @param argb32 A 32 bits ARGB color.
     * @return The blue component in [0,255].
     */
    public static int getBlue8(int argb32) {
        return argb32 & 0xFF;
    }
    
    /*
     * Derived.
     */
    
    /**
     * @param argb32 A 32 bits ARGB color.
     * @return A 32 bits ARGB with same RGB as the specified color but opaque.
     */
    public static int toOpaque(int argb32) {
        return 0xFF000000 | argb32;
    }
}
