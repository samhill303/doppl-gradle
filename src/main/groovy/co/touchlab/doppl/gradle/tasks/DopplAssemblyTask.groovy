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
import co.touchlab.doppl.gradle.DopplInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

/**
 * Copies artifacts into doppl directory structure
 */

class DopplAssemblyTask extends DefaultTask {

    BuildContext _buildContext

    @InputFiles
    FileCollection getInputFiles()
    {
        FileTree fileTree = new UnionFileTree("DopplAssemblyTask")

        BuildTypeProvider buildTypeProvider = _buildContext.getBuildTypeProvider()

        List<FileTree> sets = buildTypeProvider.sourceSets(project)
        for (FileTree set : sets) {
            fileTree.add(set)
        }

        DopplConfig dopplConfig = DopplConfig.from(project)
        if(dopplConfig.translatePattern != null) {
            fileTree = fileTree.matching(dopplConfig.translatePattern)
        }

        fileTree = fileTree.matching(TranslateTask.javaPattern {
            include "**/*.java"
        })

        return fileTree
    }

    @InputDirectory @Optional
    File getObjcDir(){
        File f = project.file(DopplInfo.SOURCEPATH_OBJC_MAIN)
        return f.exists() ? f : null
    }

    @OutputDirectory
    File getDestDopplDirFile() {
        return DopplInfo.getInstance(project).rootAssemblyFile()
    }

    @TaskAction
    void dopplDeploy(IncrementalTaskInputs inputs) {

        Utils.projectCopy(project, {
            from getInputFiles()
            into new File(getDestDopplDirFile(), "java")
            include '**/*.java'
        })

        File objcDir = getObjcDir()
        if (objcDir != null) {
            Utils.projectCopy(project, {
                from objcDir
                into new File(getDestDopplDirFile(), "src")
            })
        }

    }
}
