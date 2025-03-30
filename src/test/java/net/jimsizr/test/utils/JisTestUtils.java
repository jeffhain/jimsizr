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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.jimsizr.utils.Argb32;
import net.jimsizr.utils.BihTestUtils;
import net.jimsizr.utils.BufferedImageHelper;
import net.jimsizr.utils.JisColorUtils;
import net.jimsizr.utils.JisUtils;

public class JisTestUtils {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyDefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger THREAD_NUM_PROVIDER =
            new AtomicInteger();
        public MyDefaultThreadFactory() {
        }
        @Override
        public Thread newThread(Runnable runnable) {
            final int threadNum = THREAD_NUM_PROVIDER.incrementAndGet();
            final String threadName =
                JisTestUtils.class.getClass().getSimpleName()
                + "-PRL-"
                + threadNum;
            
            final Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            
            return thread;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double NO_CSN_MIN_BOUND_INCL = 1e-3;
    private static final double NO_CSN_MAX_BOUND_EXCL = 1e7;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisTestUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return An executor using daemon threads.
     */
    public static Executor newPrlExec(int parallelism) {
        /*
         * ForkJoinPool also works, might be slower in some cases,
         * especially when used from a non-worker thread,
         * since it's not optimized for latency but for throughput.
         */
        return new ThreadPoolExecutor(
            parallelism,
            parallelism,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new MyDefaultThreadFactory());
    }
    
    public static void shutdownNow(Executor parallelExecutor) {
        // Best effort.
        if (parallelExecutor instanceof ExecutorService) {
            ((ExecutorService) parallelExecutor).shutdownNow();
        }
    }
    
    /**
     * @param withAllCustom False for only having one alpha-premultiplied
     *        and one non-alpha-premultiplied TYPE_CUSTOM images.
     *        Allows for tests to go faster, when testing code
     *        that does not deal with TYPE_CUSTOM details
     *        other than alpha-premultiplication.
     */
    public static List<TestImageTypeEnum> newImageTypeEnumList(
        boolean withAllCustom,
        boolean withGray,
        boolean withBinary,
        boolean withIndexed) {
        final List<TestImageTypeEnum> ret = new ArrayList<>();
        //
        ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR);
        if (withAllCustom) {
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_RGBA);
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_BGRA);
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB);
        }
        //
        ret.add(TestImageTypeEnum.TYPE_INT_RGB);
        ret.add(TestImageTypeEnum.TYPE_INT_ARGB);
        ret.add(TestImageTypeEnum.TYPE_INT_ARGB_PRE);
        //
        ret.add(TestImageTypeEnum.TYPE_INT_BGR);
        //
        ret.add(TestImageTypeEnum.TYPE_3BYTE_BGR);
        ret.add(TestImageTypeEnum.TYPE_4BYTE_ABGR);
        ret.add(TestImageTypeEnum.TYPE_4BYTE_ABGR_PRE);
        //
        ret.add(TestImageTypeEnum.TYPE_USHORT_565_RGB);
        ret.add(TestImageTypeEnum.TYPE_USHORT_555_RGB);
        //
        if (withGray) {
            ret.add(TestImageTypeEnum.TYPE_BYTE_GRAY);
            ret.add(TestImageTypeEnum.TYPE_USHORT_GRAY);
        }
        //
        if (withBinary) {
            ret.add(TestImageTypeEnum.TYPE_BYTE_BINARY);
        }
        if (withIndexed) {
            ret.add(TestImageTypeEnum.TYPE_BYTE_INDEXED);
        }
        //
        if (withAllCustom) {
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_RGBA_PRE);
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_BGRA_PRE);
            ret.add(TestImageTypeEnum.TYPE_CUSTOM_INT_GRAB_PRE);
        }
        return Collections.unmodifiableList(ret);
    }
    
    /*
     * 
     */
    
    public static BufferedImage loadImage(File file) {
        final BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }
    
    /*
     * 
     */
    
    public static double randomMinMax(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    /**
     * Makes sure close pixels don't have too different values.
     * 
     * @param opaque If image has no alpha, will have no effect.
     */
    public static void randomizeCloseNeighboors(
        Random random,
        BufferedImage image,
        int maxOrthoNeighboorDelta,
        boolean opaque,
        int minTranslucentAlpha8) {
        
        final BufferedImageHelper imageHelper =
            new BufferedImageHelper(image);
        final int width = image.getWidth();
        final int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                
                /*
                 * Each pixel not too different from
                 * the one above and before,
                 * for each pixel not be too different from
                 * its neighboors, else bicubic scaling
                 * might cause large deltas (in particular
                 * in case of low alpha).
                 */
                final int argb32 =
                    newRandomArgb32Pixel(
                        random,
                        maxOrthoNeighboorDelta,
                        opaque,
                        minTranslucentAlpha8,
                        imageHelper,
                        x,
                        y);
                
                imageHelper.setNonPremulArgb32At(x, y, argb32);
            }
        }
    }
    
    /*
     * 
     */
    
    public static void checkIsValidPremulArgb32(int premulArgb32) {
        final int a8 = Argb32.getAlpha8(premulArgb32);
        final int r8 = Argb32.getRed8(premulArgb32);
        final int g8 = Argb32.getGreen8(premulArgb32);
        final int b8 = Argb32.getBlue8(premulArgb32);
        JisColorUtils.checkValidPremulAxyz(a8, r8, g8, b8);
    }
    
    /**
     * If actual image is of type TYPE_BYTE_BINARY,
     * to avoid fails due to edge cases around threshold,
     * computes the ratio of white pixels of both images,
     * and converts it into a 8-bits component as if it was
     * a floating-point color in [0,1].
     * That works as long as the image is not too small.
     */
    public static int computeMaxCptDelta(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        final int ret;
        if (expectedDstHelper.getImageType() == BufferedImage.TYPE_BYTE_BINARY) {
            ret = computeMaxCptDelta_binary(
                expectedDstHelper,
                actualDstHelper,
                neighborDelta);
        } else {
            ret = computeMaxCptDelta_notBinary(
                expectedDstHelper,
                actualDstHelper,
                neighborDelta);
        }
        
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for image copy correctness,
     * other than for exception checks.
     */
    public static void copyImage_reference(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        
        // Will throw if not same dimensions.
        final int width = Math.max(srcHelper.getWidth(), dstHelper.getWidth());
        final int height = Math.max(srcHelper.getHeight(), dstHelper.getHeight());
        
        BihTestUtils.copyImage_reference(
            srcHelper,
            0,
            0,
            dstHelper,
            0,
            0,
            width,
            height);
    }
    
    /*
     * 
     */
    
    public static String toStringTimeMeasureNs(long measureNs) {
        return toStringNoCSN(measureNs / (long) 1e3 / 1e6);
    }

    /**
     * @param value A double value.
     * @return String representing the specified value,
     *         not in "computerized scientific notation",
     *         which Double.toString(double) uses for non-infinite
     *         values, when |value| < 1e-3 or |value| >= 1e7.
     */
    public static String toStringNoCSN(double value) {
        // Quick case.
        // Should also work with long instead of int,
        // but less obvious (due to roundings...),
        // and we just want to speed up the more common
        // case of "small" integer values.
        final int intValue = (int)value;
        if (value == intValue) {
            if (value == 0.0) {
                if (Double.doubleToRawLongBits(value) < 0) {
                    return "-0.0";
                } else {
                    return "0.0";
                }
            } else {
                return Integer.toString(intValue) + ".0";
            }
        }

        final String raw = Double.toString(value);
        final double abs = Math.abs(value);
        if (abs >= NO_CSN_MAX_BOUND_EXCL) {
            if (abs == Double.POSITIVE_INFINITY) {
                return raw;
            }
            /*
             * 0123456789
             * 1.234567E5 ===> 123456.7
             * 1.23456E5  ===> 123456.0 (adding 0)
             * 1.23E5     ===> 123000.0
             * 1.0E5      ===> 100000.0
             */
            // "." close to start, so using indexOf.
            final int dotIndex = raw.indexOf('.');
            // "E" close to end, so using lastIndexOf.
            final int eIndex = raw.lastIndexOf('E');
            final int powerOfTen = Integer.parseInt(raw.substring(eIndex + 1));
            final int nbrOfSignificantLoDigits = (eIndex - dotIndex - 1);
            final int nbrOfZerosToAddBeforeDot = (powerOfTen - nbrOfSignificantLoDigits);

            int start;
            int end;

            final StringBuilder sb = new StringBuilder();
            sb.append(raw,0,dotIndex);
            if (nbrOfZerosToAddBeforeDot >= 0) {
                // Can copy all digits that were between '.' and 'E'.
                sb.append(raw, dotIndex + 1, eIndex);
                for (int i = 0; i < nbrOfZerosToAddBeforeDot; i++) {
                    sb.append('0');
                }
                sb.append(".0");
            } else {
                start = dotIndex+1;
                sb.append(raw, start, end = start + powerOfTen);
                
                sb.append('.');
                
                start = end;
                sb.append(raw,start,end = eIndex);
            }
            return sb.toString();
        } else if (abs < NO_CSN_MIN_BOUND_INCL) {
            // Not +-0.0 since already handled.
            /*
             * 01234567
             * 1.234E-4 ===> 0.0001234
             * 1.0E-4   ===> 0.0001
             */
            // "." close to start, so using indexOf.
            final int dotIndex = raw.indexOf('.');
            // "E" close to end, so using lastIndexOf.
            final int eIndex = raw.lastIndexOf('E');
            // Negative.
            final int powerOfTen = Integer.parseInt(raw.substring(eIndex+1));
            final int nbrOfZerosToAddAfterDot = (-powerOfTen-1);
            
            final StringBuilder sb = new StringBuilder();
            if (value < 0.0) {
                sb.append("-0.");
            } else {
                sb.append("0.");
            }
            for (int i = 0; i < nbrOfZerosToAddAfterDot; i++) {
                sb.append('0');
            }
            // First raw digit.
            sb.append(raw,dotIndex-1,dotIndex);
            if ((eIndex == dotIndex + 2) && (raw.charAt(dotIndex+1) == '0')) {
                // Char past dot is alone and '0': no need to add it.
            } else {
                // Raw digits that were past dot.
                sb.append(raw, dotIndex + 1, eIndex);
            }
            return sb.toString();
        } else {
            // abs in [0.001,1e7[.
            if ((abs < 1.0) && (raw.charAt(raw.length()-1) == '0')) {
                // Workaround for bug 4428022 (Double.toString(0.004) returns
                // "0.0040", same with 0.001 etc.).
                return raw.substring(0, raw.length()-1);
            } else {
                return raw;
            }
        }
    }
    
    public static BufferedImageHelper newSameTypeTinyImageAndHelper(
        BufferedImageHelper helper) {
        // Area 16 times smaller.
        final int spanDivisor = 4;
        return newSameTypeSmallerImageAndHelper(helper, spanDivisor);
    }
    
    /**
     * @param spanDivisor Must be >= 1.
     */
    public static BufferedImageHelper newSameTypeSmallerImageAndHelper(
        BufferedImageHelper helper,
        int spanDivisor) {
        final int newWidth = Math.max(1, helper.getWidth() / spanDivisor);
        final int newHeight = Math.max(1, helper.getHeight() / spanDivisor);
        return BihTestUtils.newSameTypeImageAndHelper(helper, newWidth, newHeight);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param opaque If image has no alpha, will have no effect.
     */
    private static int newRandomArgb32Pixel(
        Random random,
        int maxOrthoNeighboorDelta,
        boolean opaque,
        int minTranslucentAlpha8,
        BufferedImageHelper srcHelper,
        int x,
        int y) {
        
        final int fallbackArgb32 = 0xFF888888;
        final int topArgb32 =
            (y != 0) ? srcHelper.getNonPremulArgb32At(x, y-1)
                : fallbackArgb32;
        final int leftArgb32 =
            (x != 0) ? srcHelper.getNonPremulArgb32At(x-1, y)
                : fallbackArgb32;
        
        // Half the time adding 1 before division by 2,
        // to avoid bias towards zero due to remainer ignoring.
        final int divBonus = (random.nextBoolean() ? 1 : 0);
        
        final int maxDelta = maxOrthoNeighboorDelta;
        int a8;
        if (opaque) {
            a8 = 0xFF;
        } else {
            a8 = ((Argb32.getAlpha8(topArgb32)
                + Argb32.getAlpha8(leftArgb32)
                + divBonus) >> 1);
            a8 += randomMinMax(random, -maxDelta, maxDelta);
            a8 = JisUtils.toRange(minTranslucentAlpha8, 0xFF, a8);
        }
        
        int r8 = ((Argb32.getRed8(topArgb32)
            + Argb32.getRed8(leftArgb32)
            + divBonus) >> 1);
        int g8 = ((Argb32.getGreen8(topArgb32)
            + Argb32.getGreen8(leftArgb32)
            + divBonus) >> 1);
        int b8 = ((Argb32.getBlue8(topArgb32)
            + Argb32.getBlue8(leftArgb32)
            + divBonus) >> 1);
        r8 += randomMinMax(random, -maxDelta, maxDelta);
        g8 += randomMinMax(random, -maxDelta, maxDelta);
        b8 += randomMinMax(random, -maxDelta, maxDelta);
        r8 = JisUtils.toRange(0, 0xFF, r8);
        g8 = JisUtils.toRange(0, 0xFF, g8);
        b8 = JisUtils.toRange(0, 0xFF, b8);
        
        return JisColorUtils.toAbcd32_noCheck(a8, r8, g8, b8);
    }
    
    /*
     * 
     */
    
    private static int computeMaxCptDelta_binary(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        if ((expectedDstHelper.getImageType() != BufferedImage.TYPE_BYTE_BINARY)
            || (actualDstHelper.getImageType() != BufferedImage.TYPE_BYTE_BINARY)) {
            throw new IllegalArgumentException();
        }
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        int diffCount = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                final int minNearbyDelta =
                    getMinNearbyMaxCptDelta(
                        x,
                        y,
                        actualArgb32,
                        expectedDstHelper,
                        neighborDelta);
                if (minNearbyDelta != 0) {
                    diffCount++;
                }
            }
        }
        
        final int area = w * h;
        return BihTestUtils.getBinaryCpt8Delta(
            area,
            diffCount);
    }
    
    private static int computeMaxCptDelta_notBinary(
        BufferedImageHelper expectedDstHelper,
        BufferedImageHelper actualDstHelper,
        int neighborDelta) {
        
        int ret = 0;
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        L1 : for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int actualArgb32 =
                    actualDstHelper.getNonPremulArgb32At(x, y);
                final int minNearbyDelta =
                    getMinNearbyMaxCptDelta(
                        x,
                        y,
                        actualArgb32,
                        expectedDstHelper,
                        neighborDelta);
                ret = Math.max(ret, minNearbyDelta);
                if (ret == 0xFF) {
                    // Can't be worse.
                    break L1;
                }
            }
        }
        
        return ret;
    }
    
    /**
     * Computes minOnPixels(maxForPixelOnCpts()).
     * 
     * @return The min, on each compared pixel, of the max delta
     *         between each component.
     */
    private static int getMinNearbyMaxCptDelta(
        int x,
        int y,
        int actualDstArgb32,
        BufferedImageHelper expectedDstHelper,
        int neighborDelta) {
        
        final int w = expectedDstHelper.getWidth();
        final int h = expectedDstHelper.getHeight();
        
        int minNearbyDelta = Integer.MAX_VALUE;
        L1 : for (int j = -neighborDelta; j <= neighborDelta; j++) {
            final int yy = JisUtils.toRange(0, h-1, y + j);
            for (int i = -neighborDelta; i <= neighborDelta; i++) {
                final int xx = JisUtils.toRange(0, w-1, x + i);
                final int expectedArgb32 =
                    expectedDstHelper.getNonPremulArgb32At(xx, yy);
                final int pixelMaxCptDelta =
                    BihTestUtils.getMaxCptDelta(
                        expectedArgb32,
                        actualDstArgb32);
                minNearbyDelta = Math.min(
                    minNearbyDelta,
                    pixelMaxCptDelta);
                if (minNearbyDelta == 0) {
                    break L1;
                }
            }
        }
        
        return minNearbyDelta;
    }
}
