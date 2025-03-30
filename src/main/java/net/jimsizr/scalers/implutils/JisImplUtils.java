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
package net.jimsizr.scalers.implutils;

import java.awt.image.BufferedImage;

import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.BufferedImageHelper.BihPixelFormat;

public class JisImplUtils {
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisImplUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses direct helper constructor
     * for TYPE_INT_XXX types used in InterKind.
     * 
     * @param imageType Must be a standard image type,
     *        and not TYPE_CUSTOM.
     * @return A new helper with its (imageType) image.
     */
    public static BufferedImageHelper newHelperAndImage(
        int width,
        int height,
        int imageType) {
        
        final BufferedImageHelper ret;
        if ((imageType == BufferedImage.TYPE_INT_ARGB_PRE)
            || (imageType == BufferedImage.TYPE_INT_ARGB)
            || (imageType == BufferedImage.TYPE_INT_RGB)) {
            final int scanlineStride = width;
            final BihPixelFormat pixelFormat;
            if (imageType == BufferedImage.TYPE_INT_RGB) {
                pixelFormat = BihPixelFormat.XRGB24;
            } else {
                pixelFormat = BihPixelFormat.ARGB32;
            }
            final boolean premul =
                (imageType == BufferedImage.TYPE_INT_ARGB_PRE);
            return new BufferedImageHelper(
                null,
                scanlineStride,
                width,
                height,
                pixelFormat,
                premul);
        } else {
            final BufferedImage image =
                new BufferedImage(
                    width,
                    height,
                    imageType);
            ret = new BufferedImageHelper(image);
        }
        return ret;
    }
}
