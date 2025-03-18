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
package net.jimsizr.scalers.smart;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.types.AlgoBrand;
import net.jimsizr.types.InterKind;
import net.jimsizr.types.ResizeAlgo;
import net.jimsizr.types.ResizeAlgoType;
import net.jimsizr.utils.BufferedImageHelper;

/**
 * Class for implementing smart scalers for actual resize operations
 * (for copy, use ScalerCopySmart).
 * 
 * Smart scaler means a scaler choosing, for a specified algorithm type,
 * between AWT and JIS scalers, and eventually using intermediary images
 * of different types, to be both fast and/or accurate.
 */
public class SmartResizeScaler implements InterfaceScaler {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final InterfaceScaler SCALER_COPY_SMART = new ScalerCopySmart();
    
    /*
     * 
     */
    
    private final ResizeAlgoType algoType;
    private final InterfaceScaler scalerAwt;
    private final InterfaceScaler scalerJis;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public SmartResizeScaler(
        final ResizeAlgoType algoType,
        final InterfaceScaler scalerAwt,
        final InterfaceScaler scalerJis) {
        this.algoType = algoType;
        this.scalerAwt = scalerAwt;
        this.scalerJis = scalerJis;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "[" + this.algoType
            + ",awt=" + this.scalerAwt
            + ",jis=" + this.scalerJis
            + "]";
    }
    
    @Override
    public void scaleImage(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        Executor parallelExecutor) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int srcImageType = srcImage.getType();
        final int dstImageType = dstImage.getType();
        
        final boolean srcPremul = srcImage.isAlphaPremultiplied();
        final boolean dstPremul = dstImage.isAlphaPremultiplied();
        
        final int srcWidth = srcImage.getWidth();
        final int srcHeight = srcImage.getHeight();
        
        final int dstWidth = dstImage.getWidth();
        final int dstHeight = dstImage.getHeight();
        
        final boolean prlElseSeq = (parallelExecutor != null);
        
        final ResizeAlgo resizeAlgo =
            BestResizeAlgoHelper.getBestResizeAlgo(
                srcImageType,
                srcPremul,
                //
                dstImageType,
                dstPremul,
                //
                this.algoType,
                //
                srcWidth,
                srcHeight,
                dstWidth,
                dstHeight,
                //
                prlElseSeq);
        
        final InterKind srcIK = resizeAlgo.getSrcIK();
        final AlgoBrand algoBrand = resizeAlgo.getBrand();
        final InterKind dstIK = resizeAlgo.getDstIK();
        
        /*
         * 
         */
        
        final BufferedImageHelper srcHelperForResize =
            getHelperForResize(
                srcIK,
                srcHelper);
        if (srcHelperForResize != srcHelper) {
            SCALER_COPY_SMART.scaleImage(
                srcHelper,
                srcHelperForResize,
                parallelExecutor);
        }
        
        final BufferedImageHelper dstHelperForResize =
            getHelperForResize(
                dstIK,
                dstHelper);
        
        final InterfaceScaler scaler;
        if (algoBrand == AlgoBrand.AWT) {
            scaler = this.scalerAwt;
        } else if (algoBrand == AlgoBrand.JIS) {
            scaler = this.scalerJis;
        } else {
            throw new IllegalArgumentException("" + algoBrand);
        }
        scaler.scaleImage(
            srcHelperForResize,
            dstHelperForResize,
            parallelExecutor);
        
        if (dstHelperForResize != dstHelper) {
            SCALER_COPY_SMART.scaleImage(
                dstHelperForResize,
                dstHelper,
                parallelExecutor);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static BufferedImageHelper getHelperForResize(
        InterKind interKind,
        BufferedImageHelper helper) {
        
        final BufferedImageHelper ret;
        if (interKind == InterKind.NONE) {
            ret = helper;
        } else {
            final BufferedImage imageForResize =
                new BufferedImage(
                    helper.getWidth(),
                    helper.getHeight(),
                    interKind.getImageType());
            ret = new BufferedImageHelper(
                imageForResize);
        }
        return ret;
    }
}
