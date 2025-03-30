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
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Uses scaler1 for downscaling part,
 * and scaler2 for upscaling part.
 */
public class Scaler1Down2Up implements InterfaceScaler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceScaler scaler1;
    private final InterfaceScaler scaler2;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public Scaler1Down2Up(
        InterfaceScaler scaler1,
        InterfaceScaler scaler2) {
        this.scaler1 = scaler1;
        this.scaler2 = scaler2;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[down=" + this.scaler1 + ",up=" + this.scaler2 + "]";
    }
    
    @Override
    public void scaleImage(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int sw = srcImage.getWidth();
        final int sh = srcImage.getHeight();
        final int dw = dstImage.getWidth();
        final int dh = dstImage.getHeight();
        
        final boolean wUp = (dw > sw);
        final boolean wDown = (dw < sw);
        final boolean hUp = (dh > sh);
        final boolean hDown = (dh < sh);
        
        if ((wUp && hDown) || (wDown && hUp)) {
            /*
             * Got both downscaling and upscaling.
             * First shrinking with scaler1,
             * then growing with scaler2.
             */
            final BufferedImageHelper tmpHelper =
                JisImplUtils.newHelperAndImage(
                    (wDown ? dw : sw),
                    (hDown ? dh : sh),
                    BufferedImage.TYPE_INT_ARGB_PRE);
            this.scaler1.scaleImage(
                srcHelper,
                tmpHelper,
                parallelExecutor);
            this.scaler2.scaleImage(
                tmpHelper,
                dstHelper,
                parallelExecutor);
        } else {
            final InterfaceScaler scaler;
            if (wDown || hDown) {
                /*
                 * Some downscaling (and no upscaling).
                 */
                scaler = this.scaler1;
            } else {
                /*
                 * Some upscaling (and no downscaling),
                 * or no scaling.
                 */
                scaler = this.scaler2;
            }
            scaler.scaleImage(
                srcHelper,
                dstHelper,
                parallelExecutor);
        }
    }
}
