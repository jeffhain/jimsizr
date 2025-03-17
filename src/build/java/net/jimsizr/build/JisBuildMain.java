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
import net.jadecy.names.InterfaceNameFilter;
import net.jadecy.names.NameFilters;

/**
 * Compiles sources and creates the jar.
 */
public class JisBuildMain {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String JAR_VERSION = "1.0";
    private static final String JAR_FILE_NAME = "jimsizr.jar";
    
    private static final String SOURCE_VERSION = "1.5";
    private static final String TARGET_VERSION = "1.5";
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    private JisBuildMain() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        
        final List<String> classpathList = new ArrayList<String>();
        InterfaceNameFilter classNameFilter = NameFilters.any();
        
        JisJarBuilder.buildJar(
            classpathList,
            classNameFilter,
            SOURCE_VERSION,
            TARGET_VERSION,
            JAR_VERSION,
            JAR_FILE_NAME);
    }
}
