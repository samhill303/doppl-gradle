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

package com.github.j2objccontrib.j2objcgradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet

/**
 * Resolves `j2objc*` dependencies into their `j2objc` constructs:
 * <p/>
 * <ul>
 * <li><b>j2objcTranslationClosure</b> - The plugin will translate only the subset of
 * the configuration's source jars that are actually used by this project's
 * code (via --build-closure), and
 * compile and link the translated code directly into this project's libraries.
 * Note that if multiple projects use j2objcTranslationClosure with the same
 * external library, you will likely get duplicate symbol definition errors
 * when linking them together.  Consider instead creating a separate Gradle
 * project for that external library using j2objcTranslation.
 * </li>
 * <li><b>j2objcTranslation</b> - The plugin will translate the entire source jar
 * provided in this configuration. Usually, this configuration is used
 * to translate a single external Java library into a standalone Objective C library, that
 * can then be linked (via j2objcLinkage) into your projects.
 * </li>
 * <li><b>j2objcLinkage</b> - The plugin will include the headers of, and link to
 * the static library within, the referenced project.  Usually this configuration
 * is used with other projects (your own, or external libraries translated
 * with j2objcTranslation) that the J2ObjC Gradle Plugin has also been applied to.
 * </li>
 * </ul>
 */
@CompileStatic
public class DependencyResolver {

    final Project project
    final J2objcConfig j2objcConfig

    public DependencyResolver(Project project, J2objcConfig j2objcConfig) {
        this.project = project
        this.j2objcConfig = j2objcConfig
    }

    public void configureAll() {

        //Current "lazy" plan. Just copy all dependencies. If something is changed, will need to clean.
        //TODO: Fix the lazy
        configForConfig('doppel', j2objcConfig.translateDoppelLibs)
        configForConfig('testDoppel', j2objcConfig.translateDoppelTestLibs)
    }

    void configForConfig(String configName, List<DoppelDependency> dopplDependencyList){
        def dopplConfig = project.configurations.getByName(configName)

        //Add project dependencies
        dopplConfig.dependencies.each {
            if (it instanceof ProjectDependency) {
                Project beforeProject = it.dependencyProject
                dopplDependencyList.add(new DoppelDependency(beforeProject.name, new File(J2objcConfig.from(beforeProject).getDestDoppelFolder())))
            }
        }

        //Add external "dop" file dependencies
        dopplConfig.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact ra ->
            if(ra.extension.equals("dop")) {
                def group = ra.moduleVersion.id.group
                def name = ra.moduleVersion.id.name
                def version = ra.moduleVersion.id.version
                def dependency = new DoppelDependency(group, name, version, new File(j2objcConfig.doppelDependencyExploded))

                project.copy { CopySpec cp ->
                    cp.from project.zipTree(ra.file)
                    cp.into dependency.dependencyFolderLocation()// j2objcConfig.doppelDependencyExploded + "/" + dependency.fullFolderName()
                }

                dopplDependencyList.add(dependency)
            }
        }
    }

    private static final String MAIN_EXTRACTION_TASK_NAME = 'j2objcTranslatedMainLibraryExtraction'
    private static final String TEST_EXTRACTION_TASK_NAME = 'j2objcTranslatedTestLibraryExtraction'

    /**
     * Adds to the main java sourceSet a to-be-generated directory that contains the contents
     * of `j2objcTranslation` dependency libraries (if any).
     */
    static void configureSourceSets(Project project) {
        configureSourceSet(project, "${project.buildDir}/mainTranslationExtraction", SourceSet.MAIN_SOURCE_SET_NAME,
                MAIN_EXTRACTION_TASK_NAME)
        configureSourceSet(project, "${project.buildDir}/testTranslationExtraction", SourceSet.TEST_SOURCE_SET_NAME,
                TEST_EXTRACTION_TASK_NAME)
    }

    protected static void configureSourceSet(Project project, String dir, String sourceSetName, String taskName) {
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention)
        SourceSet sourceSet = javaConvention.sourceSets.findByName(sourceSetName)
        sourceSet.java.srcDirs(project.file(dir))
        Copy copy = project.tasks.create(taskName, Copy,
                { Copy task ->
                    task.into(project.file(dir))
                    // If two libraries define the same file, fail early.
                    task.duplicatesStrategy = DuplicatesStrategy.FAIL
                })
        project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn(copy)
    }
}
