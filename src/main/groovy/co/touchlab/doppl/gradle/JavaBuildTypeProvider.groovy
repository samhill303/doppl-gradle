package co.touchlab.doppl.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * We're just going to hard code stuff for now, till we crack the java plugin's data
 * schema format. Android was a little more critical for our purposes.
 *
 * Created by kgalligan on 3/12/17.
 */
class JavaBuildTypeProvider implements BuildTypeProvider{
    @Override
    void configureDependsOn(Project project, Task prebuildTask) {
        prebuildTask.dependsOn('jar')
    }

    @Override
    List<FileTree> testSourceSets(Project project) {
        List<FileTree> sources = new ArrayList<>()
        sources.add(project.fileTree('src/test/java'))
        sources.add(project.fileTree('build/classes/test'))
        sources.add(project.fileTree('build/generated/source/apt/test'))
        return sources
    }

    @Override
    List<FileTree> sourceSets(Project project) {
        List<FileTree> sources = new ArrayList<>()
        sources.add(project.fileTree('src/main/java'))
        sources.add(project.fileTree('build/classes/main'))
        sources.add(project.fileTree('build/generated/source/apt/main'))
        return sources
    }
}
