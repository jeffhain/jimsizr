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
package net.jimsizr.build;

/**
 * Utilities about the underlying OS.
 */
public class JisOsUtils {

    /*
     * Derived from org.apache.commons.lang.SystemUtils.
     */
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * For lazy initialization.
     */
    private static class MyLazy {
        /**
         * os.name property, or "unknown" if not defined.
         */
        private static final String OS_NAME;
        static {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                osName = "unknown";
            }
            OS_NAME = osName;
        }
        
        private static final boolean IS_OS_WINDOWS = OS_NAME.startsWith("Windows");
        private static final boolean IS_OS_MAC = OS_NAME.startsWith("Mac");
    }
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisOsUtils() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return os.name property, or "unknown" if it's not defined.
     */
    public static String getOsName() {
        return MyLazy.OS_NAME;
    }
    
    /*
     * 
     */
    
    public static boolean isWindows() {
        return MyLazy.IS_OS_WINDOWS;
    }
    
    /**
     * @return True if OS is Mac (OS X or not).
     */
    public static boolean isMac() {
        return MyLazy.IS_OS_MAC;
    }
}
