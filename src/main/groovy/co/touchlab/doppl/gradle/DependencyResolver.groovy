/*
 * Original work Copyright (c) 2015 the authors of j2objc-gradle (see AUTHORS file)
 * Modified work Copyright (c) 2017 Touchlab Inc
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
 * Resolves doppl dependencies. Can handle external artifacts as well as project dependencies
 */
@CompileStatic
public class DependencyResolver {

    public static final String CONFIG_DOPPL = 'doppl'
    public static final String CONFIG_DOPPL_ONLY = 'dopplOnly'
    public static final String CONFIG_TEST_DOPPL = 'testDoppl'
    final Project project
    final DopplConfig dopplConfig
    List<DopplDependency> translateDopplLibs = new ArrayList<>()
    List<DopplDependency> translateDopplTestLibs = new ArrayList<>()

    public DependencyResolver(Project project, DopplConfig dopplConfig) {
        this.project = project
        this.dopplConfig = dopplConfig
    }

    public void configureAll() {

        Map<String, DopplDependency> dependencyMap = new HashMap<>()

        //Current "lazy" plan. Just copy all dependencies. If something is changed, will need to clean.
        //TODO: Fix the lazy
        configForConfig(CONFIG_DOPPL, translateDopplLibs, this.dopplConfig.dopplDependencyExploded, dependencyMap)
        configForConfig(CONFIG_DOPPL_ONLY, translateDopplLibs, this.dopplConfig.dopplOnlyDependencyExploded, dependencyMap)
        configForConfig(CONFIG_TEST_DOPPL, translateDopplTestLibs, this.dopplConfig.testDopplDependencyExploded, dependencyMap)
    }

    void configForConfig(String configName, List<DopplDependency> dopplDependencyList, String explodedPath, Map<String, DopplDependency> dependencyMap){
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

            def extension = ra.extension
            if(extension != null && extension.equals("dop")) {
                def group = ra.moduleVersion.id.group
                def name = ra.moduleVersion.id.name
                def version = ra.moduleVersion.id.version

                String mapKey = group + "_" + name +"_"+ version
                if(dependencyMap.containsKey(mapKey)) {
                    dopplDependencyList.add(dependencyMap.get(mapKey))
                }
                else{

                    String depFolder = explodedPath
                    def dependency = new DopplDependency(group, name, version, new File(depFolder))

                    project.copy { CopySpec cp ->
                        cp.from project.zipTree(ra.file)
                        cp.into dependency.dependencyFolderLocation()
                    }

                    dopplDependencyList.add(dependency)
                    dependencyMap.put(mapKey, dependency)
                }
            }
        }
    }
}