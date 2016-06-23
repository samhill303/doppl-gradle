package com.github.j2objccontrib.j2objcgradle.tasks

import com.github.j2objccontrib.j2objcgradle.J2objcConfig
import groovy.transform.CompileStatic
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Created by kgalligan on 6/22/16.
 */
@CompileStatic
class DoppelArchiveGroovyTask extends Jar {

    @InputDirectory
    String getDoppelDeployDir() {
        return J2objcConfig.from(project).getDoppelDeployDirectory() +"/"+ project.name
    }

    DoppelArchiveGroovyTask() {
//        classifier = 'dop'
        extension = 'dop'
        from(new File(getDoppelDeployDir()))
    }


}