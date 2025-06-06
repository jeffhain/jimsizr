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
package net.jimsizr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jadecy.comp.CompHelper;
import net.jimsizr.build.JisBinConfig;

/**
 * Helper to compile code of this library for tests.
 */
public class JisTestCompHelper {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final String SOURCE_VERSION = "1.5";
    private static final String TARGET_VERSION = "1.5";
    
    private static final String OUTPUT_DIR_PARENT_PATH = "test_comp";
    
    private static final CompHelper COMP_HELPER = new CompHelper(
            SOURCE_VERSION,
            TARGET_VERSION,
            getTestClasspathList(),
            //
            OUTPUT_DIR_PARENT_PATH);

    /*
     * In compilation order.
     */
    
    public static final String MAIN_SRC_PATH = "src/main/java";
    public static final String BUILD_SRC_PATH = "src/build/java";
    public static final String TEST_SRC_PATH = "src/test/java";
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisTestCompHelper() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static List<String> getTestClasspathList() {
        final List<String> list = new ArrayList<String>();
        
        list.addAll(JisBinConfig.getCommonClasspathList());
        
        return list;
    }
    
    /**
     * @return A new list containing all sources directories paths,
     *         in compilation order.
     */
    public static List<String> newAllSrcDirPathList() {
        return Arrays.asList(
                MAIN_SRC_PATH,
                BUILD_SRC_PATH,
                TEST_SRC_PATH);
    }
    
    /**
     * Uses CompHelper.ensureCompiledAndGetOutputDir(...) on a static instance,
     * to compile specified sources of this library.
     * 
     * @return Path of directory where the compiled class files can be found.
     */
    public static String ensureCompiledAndGetOutputDirPath(List<String> srcDirPathList) {
        if (DEBUG) {
            System.out.println("ensureCompiledAndGetOutputDirPath(" + srcDirPathList + ")");
        }
        return COMP_HELPER.ensureCompiledAndGetOutputDirPath(srcDirPathList);
    }
}
