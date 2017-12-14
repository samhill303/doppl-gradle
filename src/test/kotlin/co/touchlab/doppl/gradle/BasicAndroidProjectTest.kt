package co.touchlab.doppl.gradle

import co.touchlab.doppl.utils.replaceFile
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


class BasicAndroidProjectTest {

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
        writeRunCustomConfig(depends = """
            compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:25.1.0'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
    testCompile 'junit:junit:4.12'
    """, config = """
    translatePattern {
        include 'co/touchlab/basicandroid/shared/**'
        include 'co/touchlab/basicandroid/BasicAndroidTest.java'
        include 'co/touchlab/basicandroid/ExampleUnitTest.java'
    }

    translatedPathPrefix 'co.touchlab.basicandroid', 'BA'
    translatedPathPrefix 'co.touchlab.basicandroid.shared', 'BAS'

    testIdentifier {
        include 'co/touchlab/basicandroid/**Test.java'
    }
        """)
        assertTrue("Prefix incorrectly generated", validateFileContent(File(projectFolder, "app/build/prefixes.properties"), { s ->
            return@validateFileContent s.contains("co.touchlab.basicandroid.shared=BAS")
        }))

        val rerunResult = buildResult()
        assertEquals(rerunResult.task(":app:j2objcMainTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun testIdentifier()
    {
        writeRunCustomConfig(depends = """
            compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:25.1.0'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
    testCompile 'junit:junit:4.12'
    """, config = """
    translatePattern {
        include 'co/touchlab/basicandroid/shared/**'
        include 'co/touchlab/basicandroid/BasicAndroidTest.java'
        include 'co/touchlab/basicandroid/ExampleUnitTest.java'
    }

    translatedPathPrefix 'co.touchlab.basicandroid', 'BA'
    translatedPathPrefix 'co.touchlab.basicandroid.shared', 'BAS'

    testIdentifier {
        include 'co/touchlab/basicandroid/**Test.java'
    }
        """)
        assertTrue("Test classes not found in dopplTests.txt", validateFileContent(File(projectFolder, "app/build/dopplTests.txt"), { s ->
            return@validateFileContent s.contains("co.touchlab.basicandroid.BasicAndroidTest") && s.contains("co.touchlab.basicandroid.ExampleUnitTest")
        }))

        val rerunResult = buildResult()
        assertEquals(rerunResult.task(":app:j2objcTestTranslate").outcome, TaskOutcome.UP_TO_DATE)
    }

    private fun writeRunCustomConfig(depends: String = "", config: String = "")
    {
        replaceFile(projectFolder, "app/build.gradle", """
    //apply plugin: 'com.android.application'
    //apply plugin: 'co.doppl.gradle'

    plugins {
        id 'com.android.application'
        id 'co.doppl.gradle'
    }

    android {
        compileSdkVersion 25
        buildToolsVersion "25.0.1"
        defaultConfig {
            applicationId "co.touchlab.basicandroid"
            minSdkVersion 23
            targetSdkVersion 25
            versionCode 1
            versionName "1.0"
            testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        }
        buildTypes {
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            }
        }
    }

    dependencies {

        $depends
    }

    dopplConfig {
        $config
    }

                """)
        val result = buildResult()
        assertEquals(result.task(":app:dopplBuild").outcome, TaskOutcome.SUCCESS)
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