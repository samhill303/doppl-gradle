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
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Move java code to transpile directories
 */

class JavaStagingTask extends DefaultTask {

    FileTree sourceFileTree
    File destDir

    @InputFiles
    FileCollection getSrcFiles() {
        DopplConfig dopplConfig = DopplConfig.from(project)
        FileTree fileTree = sourceFileTree
        if(dopplConfig.translatePattern != null)
            fileTree = fileTree.matching(dopplConfig.translatePattern)
        return fileTree
    }

    @OutputDirectory
    File getDopplJavaDirFile() {
        return destDir
    }

    @TaskAction
    void stageJavaFiles(IncrementalTaskInputs inputs) {

        logger.info("DopplGradle: staging-inputs.incremental: " + inputs.incremental)
        if(inputs.incremental)
        {
            File baseDir = (File)sourceFileTree.dir

            inputs.outOfDate(new Action<InputFileDetails>() {
                @Override
                void execute(InputFileDetails details) {
                    String subPath = Utils.relativePath(baseDir, details.file)// details.file.getPath().substring(baseDir.getPath().length())
                    File newFile = new File(getDopplJavaDirFile(), subPath)

                    project.copy {
                        from details.file
                        into newFile.getParentFile()
                        include '**/*.java'
                    }
                }
            })

            inputs.removed(new Action<InputFileDetails>() {
                @Override
                void execute(InputFileDetails details) {
                    String subPath = details.file.getPath().substring(baseDir.getPath().length())
                    File newFile = new File(getDopplJavaDirFile(), subPath)

                    if(newFile.exists())
                        newFile.delete()
                }
            })
        }
        else {
            Utils.projectCopy(project, {
                from getSrcFiles()
                into getDopplJavaDirFile()
                includeEmptyDirs = false
                include '**/*.java'
            })
        }
    }
}
