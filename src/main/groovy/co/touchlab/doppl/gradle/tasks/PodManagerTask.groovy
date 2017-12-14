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
import co.touchlab.doppl.gradle.BuildTypeProvider
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import co.touchlab.doppl.gradle.DopplInfo
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class PodManagerTask extends DefaultTask{

    BuildContext _buildContext
    boolean testBuild
    String podfilePath

    static void addPodManagerTask(TaskContainer tasks, String path, BuildContext buildContext, boolean testBuild, Task downstream, Task upstream, int count)
    {
        Task task = tasks.create(name: "podManagerTask_${testBuild}_${count}", type: PodManagerTask){
            _buildContext = buildContext
            testBuild = true
            podfilePath = path
        }
        downstream.dependsOn(task)
        task.dependsOn(upstream)
    }

    static String getDependencyList(BuildContext _buildContext, boolean testBuild)
    {
        StringBuilder sb = new StringBuilder()

        appendDependencyNames(_buildContext.getDependencyResolver().translateDopplLibs, sb)
        if(testBuild)
            appendDependencyNames(_buildContext.getDependencyResolver().translateDopplTestLibs, sb)

        return sb.toString()
    }

    @Input
    String getDependencyList()
    {
        return getDependencyList(_buildContext, testBuild)
    }

    @Input
    String getInputFiles()
    {
        FileTree fileTree = new UnionFileTree("TranslateTask - ${(testBuild ? "test" : "main")}")

        BuildTypeProvider buildTypeProvider = _buildContext.getBuildTypeProvider()
        List<File> allFiles = new ArrayList<File>()

        for (FileTree tree : buildTypeProvider.sourceSets(project)) {
            fileTree.add(tree)
        }

        if(testBuild)
        {
            for (FileTree tree : buildTypeProvider.testSourceSets(project)) {
                fileTree.add(tree)
            }
        }

        DopplConfig dopplConfig = DopplConfig.from(project)
        if(dopplConfig.translatePattern != null) {
            fileTree = fileTree.matching(dopplConfig.translatePattern)
        }

        fileTree = fileTree.matching(TranslateTask.javaPattern {
            include "**/*.java"
        })

        allFiles.addAll(fileTree.getFiles())
        Collections.sort(allFiles)

        return allFiles.join(File.pathSeparator)
    }

    @TaskAction
    void rebuildPod(IncrementalTaskInputs inputs) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable "pod"

                args "install"

                setStandardOutput stdout
                setErrorOutput stderr

                setWorkingDir podfilePath
            })
        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
            stderr.close()
            stdout.close()
            if(stdout.size() > 0)
            {
                println("********* STDOUT *********")
                println new String(stdout.toByteArray())
            }
            if(stderr.size() > 0)
            {
                println("********* STDERR *********")
                println new String(stderr.toByteArray())
            }
        }
    }

    private static void appendDependencyNames(ArrayList<DopplDependency> libs, StringBuilder sb) {
        for (DopplDependency dependency : libs) {
            sb.append(dependency.dependencyFolderLocation().getName()).append("|")
        }
    }
}
