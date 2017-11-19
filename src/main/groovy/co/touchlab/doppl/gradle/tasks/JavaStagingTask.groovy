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
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

/**
 * Move java code to transpile directories
 */

class JavaStagingTask extends DefaultTask {

    boolean testBuild
    BuildContext _buildContext

    @InputFiles
    FileCollection getSrcFiles() {
        DopplConfig dopplConfig = DopplConfig.from(project)

        List<FileTree> sourceSets =
                testBuild ?
                _buildContext.getBuildTypeProvider().testSourceSets(project)
                          :
                _buildContext.getBuildTypeProvider().sourceSets(project)
        FileTree fileTree = new UnionFileTree("java SrcFiles", (Collection<? extends FileTree>) sourceSets)
        if(dopplConfig.translatePattern != null)
            fileTree = fileTree.matching(dopplConfig.translatePattern)
        return fileTree
    }

    @OutputDirectory
    File getDopplJavaDirFile() {
        return testBuild ?
        DopplConfig.from(project).getDopplJavaDirFileTest()
                :
               DopplConfig.from(project).getDopplJavaDirFileMain()
    }

    @TaskAction
    void stageJavaFiles(IncrementalTaskInputs inputs) {
        //Copy code
        Utils.projectCopy(project, {
            from getSrcFiles()
            into getDopplJavaDirFile()
            includeEmptyDirs = false
            include '**/*.java'
        })
    }
}
