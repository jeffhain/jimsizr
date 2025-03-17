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

import java.awt.image.BufferedImage;
import java.util.concurrent.Executor;

import net.jimsizr.scalers.api.InterfaceScaler;
import net.jimsizr.scalers.basic.jis.ScalerBoxsampledJis;
import net.jimsizr.scalers.implutils.ScalerPreDown1Then2;
import net.jimsizr.scalers.implutils.Scaler1Down2Up;
import net.jimsizr.scalers.implutils.ScalerIterForDown;
import net.jimsizr.scalers.smart.ScalerBicubicSmart;
import net.jimsizr.scalers.smart.ScalerBilinearSmart;
import net.jimsizr.scalers.smart.ScalerNearestSmart;
import net.jimsizr.scalers.smart.copy.ScalerCopySmart;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisUtils;

/**
 * The principal API of this Jimsizr library.
 * 
 * This API offers no way to tune rendering hits,
 * which allows not to let drawImage() specificities
 * leak out through the interface, and to eventually use
 * different and/or faster algorithms (ex.: BOXSAMPLED
 * is not available in AWT).
 */
public class Jimsizr {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Bilinear uses 2x2 neighbors, so need to downscale
     * by not more than 2 to make sure all pixels contribute.
     * 
     * Bicubic uses 4x4 neighbors, but using a max downscaling of 4
     * gives bad results, far pixels not contributing much.
     * 
     * As a result, using same threshold for bilinear and bicubic.
     */
    private static final double MAX_BIX_DOWNSCALING = 2.0;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final Jimsizr INSTANCE = new Jimsizr(-1);
    
    /*
     * Our scalers are thread-safe and non-blocking,
     * so using single instances.
     */
    
    private final InterfaceScaler scalerCopy;
    
    /**
     * Scaler by ScalingType ordinal.
     */
    private final InterfaceScaler[] scalerByOrdinal;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    /**
     * @param maxAreaForSplit Used for both
     *        src/dst area threshold for split,
     *        and max slice area.
     *        If < 0, using implementation defaults.
     */
    Jimsizr(int maxAreaForSplit) {
        
        final InterfaceScaler scalerCopy;
        final InterfaceScaler scalerNearest;
        final InterfaceScaler scalerBilinear;
        final InterfaceScaler scalerBicubic;
        final InterfaceScaler scalerBoxsampled;
        if (maxAreaForSplit < 0) {
            scalerCopy = new ScalerCopySmart();
            scalerNearest = new ScalerNearestSmart();
            scalerBilinear = new ScalerBilinearSmart();
            scalerBicubic = new ScalerBicubicSmart();
            scalerBoxsampled = new ScalerBoxsampledJis();
        } else {
            scalerCopy = new ScalerCopySmart(
                maxAreaForSplit,
                maxAreaForSplit);
            scalerNearest = new ScalerNearestSmart(maxAreaForSplit);
            scalerBilinear = new ScalerBilinearSmart(maxAreaForSplit);
            scalerBicubic = new ScalerBicubicSmart(maxAreaForSplit);
            scalerBoxsampled = new ScalerBoxsampledJis(
                maxAreaForSplit,
                maxAreaForSplit);
        }
        
        final InterfaceScaler scalerIterativeBilinear = new ScalerIterForDown(
            scalerBilinear,
            MAX_BIX_DOWNSCALING);
        final InterfaceScaler scalerIterativeBicubic = new ScalerIterForDown(
            scalerBicubic,
            MAX_BIX_DOWNSCALING);
        
        final InterfaceScaler[] scalerByOrdinal =
            new InterfaceScaler[ScalingType.values().length];
        scalerByOrdinal[ScalingType.NEAREST.ordinal()] = scalerNearest;
        scalerByOrdinal[ScalingType.BILINEAR.ordinal()] = scalerBilinear;
        scalerByOrdinal[ScalingType.BICUBIC.ordinal()] = scalerBicubic;
        scalerByOrdinal[ScalingType.BOXSAMPLED.ordinal()] = scalerBoxsampled;
        scalerByOrdinal[ScalingType.ITERATIVE_BILINEAR.ordinal()] = scalerIterativeBilinear;
        scalerByOrdinal[ScalingType.ITERATIVE_BICUBIC.ordinal()] = scalerIterativeBicubic;
        
        /*
         * 
         */
        
        this.scalerCopy = scalerCopy;
        
        this.scalerByOrdinal = scalerByOrdinal;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Convenience method, delegating to the other resize() method and
     * always using true for its mustDownscaleFullyWithFirstType,
     * allowSrcArrDirectUse and allowDstArrDirectUse arguments.
     * 
     * @param downscalingType Scaling type to use for downscaling.
     * @param upscalingType Scaling type to use for upscaling.
     * @param srcImage (in) Source image.
     * @param dstImage (out) Destination image.
     * @param parallelExecutor If not null, used for parallelization
     *        (so should have at least two worker threads).
     * @throws NullPointerException if any scaling type or image is null.
     * @throws IllegalArgumentException if images are the same object.
     */
    public static void resize(
        ScalingType downscalingType,
        ScalingType upscalingType,
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor) {
        
    	final boolean mustDownscaleFullyWithFirstType = true;
    	final boolean allowSrcArrDirectUse = true;
    	final boolean allowDstArrDirectUse = true;
    	resize(
    		downscalingType,
    		upscalingType,
            srcImage,
            dstImage,
            parallelExecutor,
            mustDownscaleFullyWithFirstType,
            allowSrcArrDirectUse,
            allowDstArrDirectUse);
    }
    
    /**
     * For general images, a good choice for both quality and speed
     * is either BOXSAMPLED or ITERATIVE_BILINEAR for downscaling,
     * and BICUBIC for upscaling.
     * For pixel art, BOXSAMPLED or ITERATIVE_BILINEAR can be used
     * for downscaling, and BOXSAMPLED for upscaling,
     * or possibly NEAREST to make huge upscalings quicker.
     * 
     * Using false for mustDownscaleFullyWithFirstType:
     * - In case of large downscaling, and using BOXSAMPLED or ITERATIVE_BILINEAR
     *   as first scaling type and BICUBIC as second scaling type, allows for
     *   accurate downscaling while still benefiting from a bit of sharpness
     *   from BICUBIC for the final downscaling step.
     * - In case of small downscaling, to resize efficiently in only one pass
     *   using only the second scaling type.
     * 
     * @param firstScalingType Scaling type to use for the first scaling step,
     *        which only involves downscaling.
     * @param secondScalingType Scaling type to use for the remaining scaling.
     * @param srcImage (in) Source image.
     * @param dstImage (out) Destination image.
     * @param parallelExecutor If not null, used for parallelization
     *        (so should have at least two worker threads).
     * @param mustDownscaleFullyWithFirstType True to use first scaling type
     *        for all downscaling, false to only use it for downscaling
     *        until remaining downscaling divides spans by less than two.
     * @param allowSrcArrDirectUse True for best resizing performances,
     *        false to avoid eventually directly using image internal array
     *        (to avoid call to "theTrackable.setUntrackable()").
     * @param allowDstArrDirectUse True for best resizing performances,
     *        false to avoid eventually directly using image internal array
     *        (to avoid call to "theTrackable.setUntrackable()").
     * @throws NullPointerException if any scaling type or image is null.
     * @throws IllegalArgumentException if images are the same object.
     */
    public static void resize(
        ScalingType firstScalingType,
        ScalingType secondScalingType,
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor,
        //
        boolean mustDownscaleFullyWithFirstType,
        boolean allowSrcArrDirectUse,
        boolean allowDstArrDirectUse) {
        
        INSTANCE.resize_pp(
        	firstScalingType,
        	secondScalingType,
            srcImage,
            dstImage,
            parallelExecutor,
            //
        	mustDownscaleFullyWithFirstType,
            allowSrcArrDirectUse,
            allowDstArrDirectUse);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    void resize_pp(
        ScalingType firstScalingType,
        ScalingType secondScalingType,
        BufferedImage srcImage,
        BufferedImage dstImage,
        Executor parallelExecutor,
        //
        boolean mustDownscaleFullyWithFirstType,
        boolean allowSrcArrDirectUse,
        boolean allowDstArrDirectUse) {
    	
    	JisUtils.requireNonNull(firstScalingType);
    	JisUtils.requireNonNull(secondScalingType);
    	if (srcImage == dstImage) {
            throw new IllegalArgumentException(
                "images are a same object");
    	}
    	
        final BufferedImageHelper srcHelper =
            new BufferedImageHelper(
                srcImage,
                allowSrcArrDirectUse,
                allowSrcArrDirectUse);
        final BufferedImageHelper dstHelper =
            new BufferedImageHelper(
                dstImage,
                allowDstArrDirectUse,
                allowDstArrDirectUse);
        
        final InterfaceScaler scaler =
            getScaler(
            	firstScalingType,
            	secondScalingType,
                srcHelper,
                dstHelper,
                mustDownscaleFullyWithFirstType);
        
        scaler.scaleImage(
            srcHelper,
            dstHelper,
            parallelExecutor);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private InterfaceScaler getScaler(
        ScalingType scalingType1,
        ScalingType scalingType2,
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper,
        //
        boolean mustDownscaleFullyWithFirstType) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int srcWidth = srcImage.getWidth();
        final int srcHeight = srcImage.getHeight();
        final int dstWidth = dstImage.getWidth();
        final int dstHeight = dstImage.getHeight();
        
        if ((srcWidth == dstWidth)
            && (srcHeight == dstHeight)) {
            // No scaling: COPY is the fastest.
            // No ScalingType for it so we need to return.
            return this.scalerCopy;
        }
        
        /*
         * Images spans can be assumed not to be zero,
         * thanks to SampleModel constructor check.
         */
        if ((scalingType2 == ScalingType.BOXSAMPLED)
            && ((dstWidth % srcWidth) == 0)
            && ((dstHeight % srcHeight) == 0)) {
            // Pixel-aligned growth.
            // NEAREST equivalent and faster.
            scalingType1 = ScalingType.NEAREST;
            scalingType2 = ScalingType.NEAREST;
        } else {
            if (scalingType2 == ScalingType.ITERATIVE_BILINEAR) {
                // No iteration for upscaling: simplifying.
                scalingType2 = ScalingType.BILINEAR;
            } else if (scalingType2 == ScalingType.ITERATIVE_BICUBIC) {
                // No iteration for upscaling: simplifying.
                scalingType2 = ScalingType.BICUBIC;
            }
            
            /*
             * Must do this last, to properly end up for example
             * with just using ITERATIVE_BILINEAR scaler
             * if we had (ITERATIVE_BILINEAR, ITERATIVE_BILINEAR)
             * as input.
             */
            
            if ((scalingType1 == ScalingType.ITERATIVE_BILINEAR)
                && (scalingType2 == ScalingType.BILINEAR)) {
                // Can use same iterative scaler: simplifying.
                scalingType2 = scalingType1;
            } else if ((scalingType1 == ScalingType.ITERATIVE_BICUBIC)
                && (scalingType2 == ScalingType.BICUBIC)) {
                // Can use same iterative scaler: simplifying.
                scalingType2 = scalingType1;
            }
        }
        
        final InterfaceScaler scaler;
        if (scalingType1 == scalingType2) {
            scaler = this.scalerByOrdinal[scalingType1.ordinal()];
        } else {
            final InterfaceScaler scaler1 = this.scalerByOrdinal[scalingType1.ordinal()];
            final InterfaceScaler scaler2 = this.scalerByOrdinal[scalingType2.ordinal()];
            if (mustDownscaleFullyWithFirstType) {
                scaler = new Scaler1Down2Up(
                    scaler1,
                    scaler2);
            } else {
                scaler = new ScalerPreDown1Then2(
                    scaler1,
                    scaler2,
                    MAX_BIX_DOWNSCALING);
            }
        }
        
        return scaler;
    }
}
