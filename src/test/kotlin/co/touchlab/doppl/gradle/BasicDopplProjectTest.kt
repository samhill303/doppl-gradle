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


import co.touchlab.doppl.utils.findObjcClassDefinition
import co.touchlab.doppl.utils.replaceFile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class BasicDopplProjectTest: BasicTestBase() {

    @Test
    fun dopplBuildWritesClassTest()
    {
        runValidateDopplBuild()

        val mainBuildDir = File(projectFolder, "build/dopplBuild/source/out/main")
        val headerFile = File(mainBuildDir, "mainSourceOut.h")

        findObjcClassDefinition(headerFile, "CoTouchlabBasicjavaGoBasicJava")
    }

    //Check that doppl dependencies are showing up where we expect them to
    @Test
    fun checkDependencyBuild()
    {
        runGsonDependencyBuild()

        assertTrue { File(projectFolder, "build/dopplBuild/dependencies/exploded/doppl/co_doppl_com_google_code_gson_gson_2_6_2_7").exists() }
        assertTrue { File(projectFolder, "build/dopplBuild/dependencies/exploded/doppl/co_doppl_com_google_code_gson_gson_2_6_2_7/java/com/google/gson/Gson.java").exists() }

        val headerFile = File(projectFolder, "build/dopplBuild/dependencies/out/main/mainDependencyOut.h")
        findObjcClassDefinition(headerFile, "ComGoogleGsonGson")
    }

    @Test
    fun dependenciesOnlyBuildOnce()
    {
        runGsonDependencyBuild()
        val rerunResult = runProjectBuild()
        assertEquals(rerunResult.task(":dopplDependencyTranslateMain").outcome, UP_TO_DATE)
    }

    private fun runGsonDependencyBuild() {
        replaceFile(projectFolder, "build.gradle", """
    plugins {
        id 'java'
        id 'co.doppl.gradle'
    }


    group 'co.touchlab'
    version '1.2.3'

    sourceCompatibility = 1.8

    repositories {
        mavenCentral()
        maven { url 'https://dl.bintray.com/doppllib/maven2' }
    }

    dependencies {

        compile "com.google.code.gson:gson:2.6.2"
        doppl "co.doppl.com.google.code.gson:gson:2.6.2.7"

        testCompile group: 'junit', name: 'junit', version: '4.12'
    }

                """)

        runValidateDopplBuild()
    }

    private fun runValidateDopplBuild() :BuildResult{
        val result = runProjectBuild()

        assertEquals(result.task(":dopplBuild").getOutcome(), SUCCESS)

        return result
    }

    private fun runProjectBuild(): BuildResult {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("dopplBuild")
                .build()
    }
}