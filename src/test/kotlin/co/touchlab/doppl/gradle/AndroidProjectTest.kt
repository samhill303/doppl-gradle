package co.touchlab.doppl.gradle

import co.touchlab.doppl.utils.validateFileContent
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class AndroidProjectTest {

    @Rule
    @JvmField
    var testProjectDir = TemporaryFolder()

    lateinit var projectFolder: File

    @Before
    fun setup()
    {
        projectFolder = testProjectDir.newFolder()
        FileUtils.copyDirectory(File("testprojects/basicandroid"), projectFolder)
    }

    @Test
    fun translatedPathPrefix()
    {
        writeRunCustomConfig()
        assertTrue("Prefix incorrectly generated", validateFileContent(File(projectFolder, "app/build/prefixes.properties"), { s ->
            return@validateFileContent s.contains("co.touchlab.basicandroid.shared=BAS")
        }))

        val rerunResult = buildResult()
        assertEquals(rerunResult.task(":app:j2objcMainTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun testIdentifier()
    {
        writeRunCustomConfig()
        assertTrue("Test classes not found in dopplTests.txt", validateFileContent(File(projectFolder, "app/build/dopplTests.txt"), { s ->
            return@validateFileContent s.contains("co.touchlab.basicandroid.BasicAndroidTest") && s.contains("co.touchlab.basicandroid.ExampleUnitTest")
        }))

        val rerunResult = buildResult()
        assertEquals(rerunResult.task(":app:j2objcTestTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    private fun writeRunCustomConfig(depends: String = "", config: String = "")
    {
        val result = buildResult()
        assertEquals(result.task(":app:dopplBuild").outcome, TaskOutcome.SUCCESS)
    }

    private fun buildResult(): BuildResult {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("dopplBuild")
                .build()
    }
}