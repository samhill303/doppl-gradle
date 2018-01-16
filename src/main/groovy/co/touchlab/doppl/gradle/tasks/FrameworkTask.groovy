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
import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import co.touchlab.doppl.gradle.DopplInfo
import co.touchlab.doppl.gradle.FrameworkConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

class FrameworkTask extends DefaultTask {

    public boolean test
    BuildContext _buildContext

    static File podspecFile(Project project, boolean test)
    {
        return configFile(project, test, "podspec")
    }

    static File headerFile(Project project, boolean test)
    {
        String specName = podspecName(test)
        File podspecFile = new File(project.buildDir, "${specName}.h")
        return podspecFile
    }

    static File configFile(Project project, boolean test, String extension)
    {
        String specName = podspecName(test)
        File podspecFile = new File(project.projectDir, "${specName}.${extension}")
        return podspecFile
    }

    static String podspecName(boolean test) {
        test ? "testdoppllib" : "doppllib"
    }

    private List<DopplDependency> dependencyList(boolean testBuild) {
        DependencyResolver resolver = _buildContext.getDependencyResolver()
        return testBuild ? resolver.translateDopplTestLibs : resolver.translateDopplLibs
    }

    List<File> getJavaSourceFolders(boolean testBuild)
    {
        List<File> allFiles = new ArrayList<>()

        if(testBuild){
            allFiles.addAll(TranslateTask.testSourceDirs(project, _buildContext))
        }else{
            allFiles.addAll(TranslateTask.mainSourceDirs(project, _buildContext))
        }

        Collections.sort(allFiles)

        return allFiles
    }

    @TaskAction
    public void writePodspec() {
        def dopplConfig = DopplConfig.from(project)
        if(test && dopplConfig.skipTests)
            return

        String specName = podspecName(test)
        File podspecFile = podspecFile(project, test)

        List<File> objcFolders = new ArrayList<>()
        List<File> headerFolders = new ArrayList<>()
        List<File> javaFolders = new ArrayList<>()

        DopplInfo dopplInfo = DopplInfo.getInstance(project)

        if(dopplConfig.emitLineDirectives)
            javaFolders.addAll(getJavaSourceFolders(false))

        objcFolders.add(dopplInfo.dependencyOutFileMain())
        objcFolders.add(dopplInfo.sourceBuildOutFileMain())
        headerFolders.add(dopplInfo.dependencyOutFileMain())
        headerFolders.add(dopplInfo.sourceBuildOutFileMain())

        fillDependenciesFromList(dependencyList(false), objcFolders, dopplConfig.dependenciesEmitLineDirectives ? javaFolders : null)

        if(test)
        {
            if(dopplConfig.emitLineDirectives)
                javaFolders.addAll(getJavaSourceFolders(true))

            objcFolders.add(dopplInfo.dependencyOutFileTest())
            objcFolders.add(dopplInfo.sourceBuildOutFileTest())
            headerFolders.add(dopplInfo.dependencyOutFileTest())
            headerFolders.add(dopplInfo.sourceBuildOutFileTest())

            fillDependenciesFromList(dependencyList(true), objcFolders, dopplConfig.dependenciesEmitLineDirectives ? javaFolders : null)
        }

        FrameworkConfig config = test ? FrameworkConfig.findTest(project) : FrameworkConfig.findMain(project)

        File headerFile = headerFile(project, test)

        String podspecTemplate = config.podspecTemplate(
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

    private void addSourceLinks(boolean testBuild, ArrayList<File> javaFolders) {
        List<File> sourceFolders = getJavaSourceFolders(testBuild)
        javaFolders.addAll(sourceFolders)
    }

    private void fillDependenciesFromList(List<DopplDependency> mainDependencies, ArrayList<File> objcFolders,
                                          ArrayList<File> javaFolders) {
        for (DopplDependency dep : mainDependencies) {
            File sourceFolder = new File(dep.dependencyFolderLocation(), "src")
            if (sourceFolder.exists()) {
                objcFolders.add(sourceFolder)
            }
            if (javaFolders != null && dep.dependencyJavaFolder().exists()) {
                javaFolders.add(dep.dependencyJavaFolder())
            }
        }
    }
}
