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
package net.jimsizr.test.deps;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;
import net.jadecy.DepUnit;
import net.jadecy.ElemType;
import net.jadecy.Jadecy;
import net.jadecy.cmd.JadecyMain;
import net.jadecy.names.InterfaceNameFilter;
import net.jadecy.names.NameFilters;
import net.jadecy.parsing.ParsingFilters;
import net.jimsizr.build.JisBinConfig;
import net.jimsizr.test.JisTestCompHelper;

public class JisDepsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final InterfaceNameFilter CLASS_NAME_FILTER = NameFilters.any();

    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public JisDepsTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * For debug.
     */
    public static void main(String[] args) {
        if (true) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("classes cycles:");
            JadecyMain.main(new String[]{
                    JisBinConfig.getAutoCompDirPath(),
                    "-scycles",
                    "-onlystats",
            });
        }
        if (true) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("packages cycles:");
            JadecyMain.main(new String[]{
                    JisBinConfig.getAutoCompDirPath(),
                    "-packages",
                    "-scycles",
                    "-onlystats",
            });
        }
    }
    
    /*
     * 
     */

    /**
     * Tests classes cycles in all sources.
     */
    public void test_cycles_CLASS_all() {
        final String compDirPath = JisBinConfig.getAutoCompDirPath();
        final DepUnit depUnit = newDepUnit(compDirPath);
        
        depUnit.checkShortestCycles(ElemType.CLASS);
    }

    /**
     * Tests packages cycles in main sources.
     */
    public void test_cycles_PACKAGE_main() {
        final String compDirPath =
                JisTestCompHelper.ensureCompiledAndGetOutputDirPath(
                        Arrays.asList(
                                JisTestCompHelper.MAIN_SRC_PATH));
        final DepUnit depUnit = newDepUnit(compDirPath);
        
        depUnit.checkShortestCycles(ElemType.PACKAGE);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Jadecy with parsing done.
     */
    private static Jadecy newJadecy(String compDirPath) {
        // We don't care about intra-class cycles.
        final boolean mustMergeNestedClasses = true;
        final boolean apiOnly = false;
        final Jadecy jadecy = new Jadecy(
                mustMergeNestedClasses,
                apiOnly);

        ParsingFilters filters = ParsingFilters.defaultInstance();
        filters = filters.withClassNameFilter(CLASS_NAME_FILTER);

        jadecy.parser().accumulateDependencies(
                new File(compDirPath),
                filters);

        return jadecy;
    }

    private static DepUnit newDepUnit(String compDirPath) {
        return new DepUnit(newJadecy(compDirPath));
    }
}
