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

package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.BuildContext
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplInfo
import co.touchlab.doppl.gradle.FrameworkConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

class FrameworkTask extends DefaultTask {

    public boolean test

    static File podspecFile(Project project, boolean test)
    {
        return configFile(project, test, "podspec")
    }

    static File headerFile(Project project, boolean test)
    {
        return configFile(project, test, "h")
    }

    static File configFile(Project project, boolean test, String extension)
    {
        String specName = podspecName(test)
        File podspecFile = new File(project.buildDir, "${specName}.${extension}")
        return podspecFile
    }

    static String podspecName(boolean test) {
        test ? "testdoppllib" : "doppllib"
    }

    @TaskAction
    public void writePodspec() {
        if(test && DopplConfig.from(project).skipTests)
            return

        String specName = podspecName(test)
        File podspecFile = podspecFile(project, test)

        List<File> objcFolders = new ArrayList<>()
        List<File> headerFolders = new ArrayList<>()
        List<File> javaFolders = new ArrayList<>()

        DopplInfo dopplInfo = DopplInfo.getInstance(project)

        //Fill dependencies
        fillDependencyLists(dopplInfo.dependencyBuildJarFileForPhase(DopplInfo.MAIN), headerFolders, objcFolders, javaFolders)

        objcFolders.add(dopplInfo.dependencyExplodedDopplFile())
        objcFolders.add(dopplInfo.dependencyExplodedDopplOnlyFile())

        if(test)
        {
            fillDependencyLists(dopplInfo.dependencyBuildJarFileForPhase(DopplInfo.TEST), headerFolders, objcFolders, javaFolders)
            objcFolders.add(dopplInfo.dependencyExplodedTestDopplFile())
        }

        //Fill source
        fillSourceList(dopplInfo.sourceBuildJarFileMain(), objcFolders, headerFolders, javaFolders)

        if(test)
        {
            fillSourceList(dopplInfo.sourceBuildJarFileTest(), objcFolders, headerFolders, javaFolders)
        }

        FrameworkConfig config = test ? FrameworkConfig.findTest(project) : FrameworkConfig.findMain(project)

        File headerFile = headerFile(project, test)

        def podspecTemplate = config.podspecTemplate(
                project,
                headerFile,
                objcFolders,
                headerFolders,
                javaFolders,
                specName)

        BufferedWriter writer = null

        if(headerFile.exists())
            headerFile.delete()

        BufferedWriter headerWriter = new BufferedWriter(new FileWriter(headerFile))
        try {
            writer = new BufferedWriter(new FileWriter(podspecFile))
            writer.write(podspecTemplate.toString())
            for (File folder : headerFolders) {
                FileTree fileTree = project.fileTree(dir: folder, includes: ["**/*.h"])
                Set<File> files = fileTree.files
                for (File f : files) {
                    headerWriter.append("#include \"${f.getName()}\"\n")
                }
            }
        } finally {
            if (writer != null)
                writer.close()
            if(headerWriter != null)
                headerWriter.close()
        }
    }

    private void fillSourceList(File sourceFolder, ArrayList<File> objcFolders, ArrayList<File> headerFolders,
                                ArrayList<File> javaFolders) {
        objcFolders.add(sourceFolder)
        headerFolders.add(sourceFolder)
        javaFolders.add(new File(sourceFolder, DopplInfo.JAVA_SOURCE))
    }

    private void fillDependencyLists(File jarBuildOutput, ArrayList<File> headerFolders, ArrayList<File> objcFolders,
                                     ArrayList<File> javaFolders) {
        headerFolders.add(jarBuildOutput)
        objcFolders.add(jarBuildOutput)
        javaFolders.add(new File(jarBuildOutput, DopplInfo.JAVA_SOURCE))
    }
}
