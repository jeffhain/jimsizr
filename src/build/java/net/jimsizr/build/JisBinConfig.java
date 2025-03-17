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

import java.util.ArrayList;
import java.util.List;

/**
 * Installation related configuration for binaries.
 * 
 * Useful for build, and executions (tests and samples).
 */
public class JisBinConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String CONFIG_FILE_PATH;
    static {
        if (JisOsUtils.isWindows()) {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-win.properties";
        } else if (JisOsUtils.isMac()) {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-mac.properties";
        } else {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-fallback.properties";
        }
    }
    
    private static final JisPropFileUtils PROPERTIES_FILE_UTILS =
        new JisPropFileUtils(CONFIG_FILE_PATH);
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisBinConfig() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Path of java[.exe] executable.
     */
    public static String getJavaPath() {
        return PROPERTIES_FILE_UTILS.getRequiredProperty("JAVA_PATH");
    }
    
    /**
     * @return Path of directory where auto-compiled classes or jars
     *         of Jimsizr can be found.
     */
    public static String getAutoCompDirPath() {
        return PROPERTIES_FILE_UTILS.getRequiredProperty("AUTO_COMP_DIR_PATH");
    }
    
    /*
     * 
     */
    
    /**
     * @return Classpaths for all of src/build/samples/test code.
     */
    public static List<String> getCommonClasspathList() {
        return getPropValueSplitOnSemiColon("CLASSPATH_COMMON");
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static List<String> getPropValueSplitOnSemiColon(String propKey) {
        final String concatStr = PROPERTIES_FILE_UTILS.getRequiredProperty(propKey);
        final String[] strArr = concatStr.split(";");
        final List<String> strList = new ArrayList<String>();
        for (String str : strArr) {
            if (str.isEmpty()) {
                // Can cause trouble.
            } else {
                strList.add(str);
            }
        }
        return strList;
    }
}
