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

import co.touchlab.doppl.gradle.DopplConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
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

    @InputDirectory
    File dopplJavaDirFile

    @InputDirectory @Optional
    File srcObjcDir

    File srcGenMainDir

    @InputFiles
    FileTree configFiles() {
        return project.fileTree(dir: srcGenMainDir, include: ['*.mappings', 'prefixes.properties'])
    }

    @OutputDirectory
    File getDestDopplDirFile() {
        return DopplConfig.from(project).getDestDopplDirFile()
    }

    @TaskAction
    void dopplDeploy(IncrementalTaskInputs inputs) {

        //We don't really deal with incremental now. If anything is out of date, copy all. Should update this.
//        if (!inputs.incremental) {
        //Copy code

        Utils.projectCopy(project, {
            from dopplJavaDirFile
            into getDestDopplDirFile().absolutePath + "/java"
            include '**/*.java'
        })

        if (srcObjcDir) {
            Utils.projectCopy(project, {
                from srcObjcDir
                into getDestDopplDirFile().absolutePath + "/src"
            })
        }

        Utils.projectCopy(project, {
            from configFiles()
            into getDestDopplDirFile().absolutePath
            include '*.mappings'
            include 'prefixes.properties'
        })
//        }
    }
}
