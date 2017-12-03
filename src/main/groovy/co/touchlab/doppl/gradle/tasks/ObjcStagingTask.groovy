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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class ObjcStagingTask extends DefaultTask{
    FileTree sourceFileTree
    File destDir

    @InputFiles
    FileCollection getSrcFiles(){
        return sourceFileTree
    }

    @OutputDirectory
    File getDopplObjcDirFile() {
        return destDir
    }

    @TaskAction
    void stageFiles(IncrementalTaskInputs inputs)
    {
        destDir.deleteDir()
        destDir.mkdirs()

        Utils.projectCopy(project, {
            from getSrcFiles()
            into getDopplObjcDirFile()
            includeEmptyDirs = false
        })
    }
}
