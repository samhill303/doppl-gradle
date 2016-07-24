/*
 * Copyright (c) 2015 the authors of j2objc-gradle (see AUTHORS file)
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

package com.github.j2objccontrib.j2objcgradle.tasks

import com.github.j2objccontrib.j2objcgradle.J2objcConfig
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Copy doppel builds to a central directory
 */
@CompileStatic
class DoppelAssemblyTask extends DefaultTask {

    @InputFiles
    FileTree getSrcLibs() {
        return project.fileTree(dir: inputObjcCode(), include: '**/*.h') +
               project.fileTree(dir: inputArchiveDirFile()) +
               project.fileTree(dir: inputJavaJarFile()) +
               project.fileTree(dir: inputDestPodspecDir(), include: '*.podspec')
    }

    @OutputDirectory
    File getDestDoppelDirFile() {
        return J2objcConfig.from(project).getDestDoppelDirFile()
    }

    @TaskAction
    void doppelDeploy() {

        def config = J2objcConfig.from(project)

        //Copy headers
        Utils.projectCopy(project, {
            from inputObjcCode()
            into getDestDoppelDirFile().absolutePath + "/include"
            include '**/*.h'
        })

        copyLibDir("iosDebug")
        copyLibDir("iosRelease")
        copyLibDir("x86_64Debug")
        copyLibDir("x86_64Release")

        Utils.projectCopy(project, {
            from inputJavaJarFile()
            into getDestDoppelDirFile().absolutePath + "/lib"
            include '**/*.jar'
        })

        Utils.projectCopy(project, {
            from inputDestPodspecDir()
            into getDestDoppelDirFile().absolutePath
            include '*debug.podspec'
            include '*release.podspec'
        })
    }

    private void copyLibDir(String dirName)
    {
        Utils.projectCopy(project, {
            from new File(inputArchiveDirFile(), dirName)
            into getDestDoppelDirFile().absolutePath + "/lib/"+ dirName
        })
    }

    private File inputObjcCode() {
        J2objcConfig.from(project).getDestSrcDirFile('main', 'objc')
    }

    private File inputArchiveDirFile() {
        return new File(J2objcConfig.from(project).destLibDir)
    }

    private File inputJavaJarFile() {
        return new File(J2objcConfig.from(project).destJavaJarDir)
    }

    private File inputDestPodspecDir() {
        return new File(J2objcConfig.from(project).destPodspecDir)
    }
}
