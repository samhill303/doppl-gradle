package co.touchlab.doppl.gradle

import co.touchlab.doppl.utils.replaceFile
import co.touchlab.doppl.utils.validateFileContent
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class AdvancedAndroidProjectTest {

    @Rule
    @JvmField
    var testProjectDir = TemporaryFolder()

    lateinit var projectFolder: File

    private val MODULE = "MyModule"

    @Before
    fun setup()
    {
        projectFolder = testProjectDir.newFolder()
        FileUtils.copyDirectory(File("testprojects/advancedandroid"), projectFolder)
    }

    @Test
    fun translatedPathPrefix()
    {
        writeRunCustomConfig(config = """
            translatedPathPrefix 'co.touchlab.mymodule', 'MM'
            """)

        Assert.assertTrue("Prefix incorrectly generated", validateFileContent(File(projectFolder, "mymodule/build/prefixes.properties"), { s ->
            return@validateFileContent s.contains("co.touchlab.mymodule=MM")
        }))

        val rerunResult = buildResult()
        Assert.assertEquals(rerunResult.task(":$MODULE:j2objcMainTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun testIdentifier()
    {
        writeRunCustomConfig()
        Assert.assertTrue("Test classes not found in dopplTests.txt", validateFileContent(File(projectFolder, "mymodule/build/dopplTests.txt"), { s ->
            return@validateFileContent s.contains("co.touchlab.mymodule.ModuleTest")
        }))

        val rerunResult = buildResult()
        Assert.assertEquals(rerunResult.task(":$MODULE:j2objcTestTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    private fun writeRunCustomConfig(depends: String = "",
                                     config: String = "")
    {
        writeBuildFile(depends, config)
        val result = buildResult()
        for (task in result.tasks) {
            println(task)
        }
        Assert.assertEquals(result.task(":$MODULE:dopplBuild").outcome, TaskOutcome.SUCCESS)
    }

    private fun writeBuildFile(depends: String = "",
                               config: String = "")
    {
        replaceFile(projectFolder, "MyModule/build.gradle", """
    plugins {
        id 'java'
        id 'co.doppl.gradle'
    }

    group 'co.touchlab'
    version '1.2.3'

    sourceCompatibility = 1.8

    repositories {
        maven { url 'https://dl.bintray.com/doppllib/maven2' }
        mavenCentral()
    }

    dependencies {
        testCompile group: 'junit', name: 'junit', version: '4.12'
        $depends
    }

    dopplConfig {
        $config
    }
        """)
    }

    private fun buildResult(): BuildResult
    {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("dopplBuild")
                .build()
    }
}