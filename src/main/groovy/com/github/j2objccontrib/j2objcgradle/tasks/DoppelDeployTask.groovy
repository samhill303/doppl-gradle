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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Copy doppel builds to a central directory
 */
@CompileStatic
class DoppelDeployTask extends DefaultTask {


    String getDoppelDeployDir() { return J2objcConfig.from(project).doppelDeployDir }

    @Input
    String getInputFiles(){return J2objcConfig.from(project).destLibDir}

    boolean isTaskActive() { return getDoppelDeployDir() != null }

    @TaskAction
    void doppelDeploy() {

        if (!isTaskActive()) {
            logger.debug("j2objcXcode task disabled for ${project.name}")
            return
        }

        def config = J2objcConfig.from(project)

        //Copy headers
        Utils.projectCopy(project, {
            from config.getDestSrcDirFile('main', 'objc')
            into getDoppelDeployDir() + "/" + project.name + "/include"
            include '**/*.h'
        })

        Utils.projectCopy(project, {
            from config.destLibDir + "/"
            into getDoppelDeployDir() + "/" + project.name + "/lib"
//            include '**/*.a'
        })

//        Utils.projectCopy(project, {
//            from config.destLibDir + "/x86_64Debug/"
//            into getDoppelDeployDir() + "/" + project.name + "/lib/macosx"
//            include '**/*.a'
//        })


        Utils.projectCopy(project, {
            from config.destJavaJarDir
            into getDoppelDeployDir() + "/" + project.name + "/lib"
            include '**/*.jar'
        })

        Utils.projectCopy(project, {
            from config.destPodspecDir
            into getDoppelDeployDir() + "/" + project.name
            include '*.podspec'
        })
    }
}
