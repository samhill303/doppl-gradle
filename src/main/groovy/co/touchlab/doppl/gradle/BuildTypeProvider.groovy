package co.touchlab.doppl.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * Created by kgalligan on 3/11/17.
 */
interface BuildTypeProvider {
    void configureDependsOn(Project project, Task prebuildTask)

    List<FileTree> testSourceSets(Project project)

    List<FileTree> sourceSets(Project project)
}