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

package co.touchlab.basictests

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {
    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File buildFile;

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle");
    }

    @Test
    public void testHelloWorldTask() throws IOException {
        String buildFileContent = "task helloWorld {" +
                                  "    doLast {" +
                                  "        println 'Hello world!'" +
                                  "    }" +
                                  "}";
        writeFile(buildFile, buildFileContent);

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("helloWorld")
                .build();

        assertTrue(result.getOutput().contains("Hello world!"));
        assertEquals(result.task(":helloWorld").getOutcome(), SUCCESS);
    }

    @Test
    void testBasicJava() throws IOException {
        File projectFolder = testProjectDir.newFolder()
        FileUtils.copyDirectory(new File("testprojects/basicjava"), projectFolder)

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("dopplBuild")
                .build()

        BuildResult rebuildResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectFolder)
                .withArguments("build")
                .build()

//        assertTrue(result.getOutput().contains("Hello world!"));
        assertEquals(result.task(":dopplBuild").getOutcome(), SUCCESS);
//        assertEquals(rebuildResult.task(":build").getOutcome(), UP_TO_DATE);

        File mainBuildDir = new File(projectFolder, "build/dopplBuild/source/jar/main")
        File jarFile = new File(mainBuildDir, "dopplMain.jar")
        Assert.assertTrue("Jar build failed", jarFile.exists())
        File headerFile = new File(mainBuildDir, "dopplMain.h")

        findObjcClassDefinition(headerFile, "CoTouchlabBasicjavaGoBasicJava")
    }

    static boolean findObjcClassDefinition(File headerFile, String classDefinition)throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(headerFile))
        try {
            String temp = null
            while ((temp = reader.readLine()) != null)
            {
                if(temp.startsWith("@protocol ${classDefinition} ") || temp.startsWith("@interface ${classDefinition} "))
                {
                    return true
                }
            }
        } finally {
            reader.close()
        }

        return false
    }

    private void writeFile(File destination, String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(destination));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
