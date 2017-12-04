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

import co.touchlab.doppl.utils.replaceFile
import co.touchlab.doppl.utils.validateFileContent
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File


class DopplConfigTest: BasicTestBase() {

    @Test
    fun testJavaDebug()
    {
        writeRunCustomConfig(config = "javaDebug true")

        assertTrue("Line directive not found", validateFileContent(File(projectFolder, "build/dopplBuild/source/jar/main/dopplMain.m"), {s ->
            return@validateFileContent s.lines().any { it.startsWith("#line") && it.endsWith("/build/dopplBuild/source/jar/main/javasource/jar_0/co/touchlab/basicjava/GoBasicJava.java\"") }
        }))

        writeRunCustomConfig(config = "//javaDebug true")
        assertTrue("Line directives still there", validateFileContent(File(projectFolder, "build/dopplBuild/source/jar/main/dopplMain.m"), { s ->
            return@validateFileContent !s.contains("#line")
        }))
    }

    @Test
    fun testDependenciesJavaDebug()
    {
        writeRunCustomConfig(config = "dependenciesJavaDebug true", depends =
        """
            compile "com.google.code.gson:gson:2.6.2"
            doppl "co.doppl.com.google.code.gson:gson:2.6.2.7"
        """)

        val objcPath = "build/dopplBuild/dependencies/jar/main/depJar_main_0.m"
        assertTrue("Line directive not found", validateFileContent(File(projectFolder, objcPath), { s ->
            return@validateFileContent s.lines().any { it.startsWith("#line") && it.endsWith(".java\"") }
        }))

        writeRunCustomConfig(config = "//dependenciesJavaDebug true", depends =
        """
            compile "com.google.code.gson:gson:2.6.2"
            doppl "co.doppl.com.google.code.gson:gson:2.6.2.7"
        """)
        assertTrue("Line directives still there", validateFileContent(File(projectFolder, objcPath), { s ->
            return@validateFileContent s.lines().none { it.startsWith("#line") }
        }))
    }

    fun testTranslatedPathPrefix()
    {
        /*
        TODO: Test that path prefixes are working. Test both class name output and that prefixes.properties
        is written to the build dir, and that it contains whatever prefixes were defined.
        Also test that changing prefix values will trigger a rerun of project, as well as output of classes
        and prefixes.properties.
         */
    }

    fun testTranslatedPathPrefixDependencies()
    {
        /*
        TODO: Test that we can apply prefixes to dependencie
         */
    }



    fun writeRunCustomConfig(depends: String = "", config: String = "")
    {
        writeCustomConfig(depends = depends, config = config)

        val result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("dopplBuild")
                .build()

        Assert.assertEquals(result.task(":dopplBuild").getOutcome(), TaskOutcome.SUCCESS)
    }

    fun writeCustomConfig(depends: String = "", config: String = "")
    {
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
        $depends
    }

    dopplConfig {
        $config
    }

                """)
    }
}