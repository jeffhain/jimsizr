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

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger for tests, allowing to write output into a file
 * in addition to System.out.
 * 
 * Logs can be larger than console capacity, and runs
 * might take hours, and some systems reboot on their own,
 * without user consent, and so it might be better to also
 * output into a file.
 */
public class TestLogger implements Closeable {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Class<?> clazz;
    private final String outputFileRootDir;
    private final FileWriter fileWriter;
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public TestLogger(
        Class<?> clazz,
        boolean mustAlsoOutputInFile,
        String outputFileRootDir) {
        this.clazz = clazz;
        this.outputFileRootDir = outputFileRootDir;
        if (mustAlsoOutputInFile) {
            final String fileName = newOutputFilePath();
            try {
                this.fileWriter = new FileWriter(fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.fileWriter = null;
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void print(String msg) {
        System.out.print(msg);
        if (this.fileWriter != null) {
            try {
                this.fileWriter.append(msg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public void println(String msg) {
        this.print(msg);
        this.println();
    }
    
    public void println() {
        print("\n");
        // Only auto-flushing on line end.
        this.flush();
    }
    
    public void flush() {
        if (this.fileWriter != null) {
            try {
                this.fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public void close() throws IOException {
        if (this.fileWriter != null) {
            this.fileWriter.close();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private String newOutputFilePath() {
        final String datePattern = "yyyy_MM_dd_HH'h'mm'm'ss's'";
        final SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
        
        final long nowMs = System.currentTimeMillis();
        final String timeStampStr = sdf.format(new Date(nowMs));
        
        final String csn = this.clazz.getSimpleName();
        final String pkgName = this.clazz.getPackage().getName();
        final String pkgPath = this.outputFileRootDir + "/" + pkgName.replaceAll("\\.", "/");
        
        return pkgPath + "/" + csn + "_" + timeStampStr + ".log";
    }
}
