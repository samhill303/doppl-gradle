/*
 * Copyright (c) 2017 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.doppl.gradle

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.test.fail

class DopplInfoTest {
    @Test
    fun testFolderStructure()
    {
        val dopplInfo = DopplInfo.getInstance(File("."))
        checkPath(dopplInfo.rootBuildFile(), "dopplBuild")
        checkPath(dopplInfo.dependencyBuildFile(), "dopplBuild/dependencies")
        checkPath(dopplInfo.dependencyExplodedFile(), "dopplBuild/dependencies/exploded")
        checkPath(dopplInfo.dependencyExplodedDopplFile(), "dopplBuild/dependencies/exploded/doppl")
        checkPath(dopplInfo.dependencyExplodedDopplOnlyFile(), "dopplBuild/dependencies/exploded/dopplOnly")
        checkPath(dopplInfo.dependencyExplodedTestDopplFile(), "dopplBuild/dependencies/exploded/testDoppl")

        checkPath(dopplInfo.dependencyOutFile(), "dopplBuild/dependencies/out")
        checkPath(dopplInfo.dependencyOutFileMain(), "dopplBuild/dependencies/out/main")
        checkPath(dopplInfo.dependencyOutFileTest(), "dopplBuild/dependencies/out/test")
        checkPath(dopplInfo.dependencyOutMainMappings(), "dopplBuild/dependencies/out/main/j2objc.mappings")
        checkPath(dopplInfo.dependencyOutTestMappings(), "dopplBuild/dependencies/out/test/j2objc.mappings")

        checkPath(dopplInfo.sourceBuildFile(), "dopplBuild/source")

        checkPath(dopplInfo.sourceBuildObjcFile(), "dopplBuild/source/objc")
        checkPath(dopplInfo.sourceBuildObjcFileForPhase("main"), "dopplBuild/source/objc/main")
        checkPath(dopplInfo.sourceBuildObjcFileMain(), "dopplBuild/source/objc/main")
        checkPath(dopplInfo.sourceBuildObjcFileForPhase("test"), "dopplBuild/source/objc/test")
        checkPath(dopplInfo.sourceBuildObjcFileTest(), "dopplBuild/source/objc/test")

        try {
            checkPath(dopplInfo.sourceBuildObjcFileForPhase("asdf"), "dopplBuild/source/objc/asdf")
            fail("Shouldn't be allowed")
        } catch (e: IllegalArgumentException) {
        }

        checkPath(dopplInfo.sourceBuildOutFile(), "dopplBuild/source/out")
        checkPath(dopplInfo.sourceBuildOutFileMain(), "dopplBuild/source/out/main")
        checkPath(dopplInfo.sourceBuildOutFileTest(), "dopplBuild/source/out/test")
        checkPath(dopplInfo.sourceBuildOutMainMappings(), "dopplBuild/source/out/main/j2objc.mappings")
        checkPath(dopplInfo.sourceBuildOutTestMappings(), "dopplBuild/source/out/test/j2objc.mappings")
    }

    fun checkPath(f: File, path: String)
    {
        Assert.assertTrue(f.path.endsWith(path))
    }
}