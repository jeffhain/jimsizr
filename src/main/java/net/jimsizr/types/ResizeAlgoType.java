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

/**
 * No BOXSAMPLED because AWT does not have it
 * (but can be approximated on downscaling
 * with iterative bilinear, if spans are divided
 * by about the same divisor).
 */
public enum ResizeAlgoType {
    NEAREST,
    BILINEAR,
    BICUBIC;
    
    private static final int ORDINAL_BIT_SIZE =
        JisTypesInternals.unsignedBitSize(
            ResizeAlgoType.values().length - 1);
    
    public String toStringShortest() {
        switch (this) {
            case NEAREST: return "NEAR";
            case BILINEAR: return "BILI";
            case BICUBIC: return "BICU";
            default: throw new AssertionError();
        }
    }
    
    public static int ordinalBitSize() {
        return ORDINAL_BIT_SIZE;
    }
}
