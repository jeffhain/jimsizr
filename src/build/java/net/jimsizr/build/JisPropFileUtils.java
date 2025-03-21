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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;

import net.jimsizr.utils.JisUtils;

/**
 * Utilities for properties files.
 */
public class JisPropFileUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final String filePath;
    
    private final Properties properties = new Properties();
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public JisPropFileUtils(String filePath) {
        this.filePath = JisUtils.requireNonNull(filePath);
        
        try {
            final FileInputStream fis = new FileInputStream(filePath);
            try {
                this.properties.load(fis);
            } finally {
                fis.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Optional.
     */
    
    /**
     * @param key Property's key.
     * @return Property's value, or null if none.
     */
    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }
    
    /*
     * Required.
     */
    
    /**
     * @param key Property's key.
     * @return Property's value.
     * @throws NoSuchElementException if the specified property is not found. 
     */
    public String getRequiredProperty(String key) {
        final String val = this.properties.getProperty(key);
        if (val == null) {
            throw new NoSuchElementException("property " + key + " not found in file " + this.filePath);
        }
        return val;
    }
}
