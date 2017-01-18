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

package co.touchlab.doppl.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec

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

    public static final String CONFIG_DOPPL = 'doppl'
    public static final String CONFIG_TEST_DOPPL = 'testDoppl'
    final Project project
    final DopplConfig dopplConfig

    public DependencyResolver(Project project, DopplConfig dopplConfig) {
        this.project = project
        this.dopplConfig = dopplConfig
    }

    public void configureAll() {

        //Current "lazy" plan. Just copy all dependencies. If something is changed, will need to clean.
        //TODO: Fix the lazy
        configForConfig(CONFIG_DOPPL, dopplConfig.translateDopplLibs)
        configForConfig(CONFIG_TEST_DOPPL, dopplConfig.translateDopplTestLibs)
    }

    void configForConfig(String configName, List<DopplDependency> dopplDependencyList){
        def dopplConfig = project.configurations.getByName(configName)

        //Add project dependencies
        dopplConfig.dependencies.each {
            if (it instanceof ProjectDependency) {

                Project beforeProject = it.dependencyProject
                dopplDependencyList.add(new DopplDependency(beforeProject.name, new File(DopplConfig.from(beforeProject).getDestDopplFolder())))
            }
        }

        //Add external "dop" file dependencies
        dopplConfig.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact ra ->

            def classifier = ra.classifier
            if(classifier != null && classifier.equals("doppl")) {
                def group = ra.moduleVersion.id.group
                def name = ra.moduleVersion.id.name
                def version = ra.moduleVersion.id.version
                println "dopplLibs-adding: config("+ configName +")/name("+ name +")"
                def dependency = new DopplDependency(group, name, version, new File(this.dopplConfig.dopplDependencyExploded))

                project.copy { CopySpec cp ->
                    cp.from project.zipTree(ra.file)
                    cp.into dependency.dependencyFolderLocation()// dopplConfig.dopplDependencyExploded + "/" + dependency.fullFolderName()
                }

                dopplDependencyList.add(dependency)
            }
        }
    }
}
