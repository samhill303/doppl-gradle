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

import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree

/**
 * We're just going to hard code stuff for now, till we crack the java plugin's data
 * schema format. Android was a little more critical for our purposes.
 *
 * Created by kgalligan on 3/12/17.
 */
class JavaBuildTypeProvider implements BuildTypeProvider{

    private final DopplConfig dopplConfig
    private final Project project

    JavaBuildTypeProvider(Project project) {
        this.project = project
        dopplConfig = DopplConfig.from(project)
    }

    @Override
    void configureDependsOn(Project project, Task upstreamTask, Task downstreamTask) {
        Task javaCompile = project.tasks.getByName("jar")
        javaCompile.dependsOn upstreamTask
        downstreamTask.dependsOn javaCompile
    }

    @Override
    void configureTestDependsOn(Project project, Task upstreamTask, Task downstreamTask) {
        Task javaCompile = project.tasks.getByName("test")
        javaCompile.dependsOn upstreamTask
        downstreamTask.dependsOn javaCompile
    }

    @Override
    List<FileTree> testSourceSets(Project project) {
        List<FileTree> sources = new ArrayList<>()
        sources.add(project.fileTree('src/test/java'))
        sources.add(project.fileTree('build/classes/test'))
        sources.add(project.fileTree('build/generated/source/apt/test'))

        addConfigDirs(dopplConfig.generatedTestSourceDirs, sources)

        return sources
    }

    @Override
    List<FileTree> sourceSets(Project project) {
        List<FileTree> sources = new ArrayList<>()
        sources.add(project.fileTree('src/main/java'))
        sources.add(project.fileTree('build/classes/main'))
        sources.add(project.fileTree('build/generated/source/apt/main'))

        addConfigDirs(dopplConfig.generatedSourceDirs, sources)

        return sources
    }

    void addConfigDirs(
            List<String> generatedSourceDirs,
            List<FileTree> paths
    ){
        if(generatedSourceDirs.size() > 0)
        {
            List<ConfigurableFileTree> trees = Utils.javaTrees(project, generatedSourceDirs)
            for (ConfigurableFileTree tree : trees) {
                paths.add(tree)
            }
        }
    }

}
