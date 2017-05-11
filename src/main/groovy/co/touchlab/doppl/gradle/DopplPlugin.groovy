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

import co.touchlab.doppl.gradle.analytics.DopplAnalytics
import co.touchlab.doppl.gradle.tasks.DeployTask
import co.touchlab.doppl.gradle.tasks.DopplAssemblyTask
import co.touchlab.doppl.gradle.tasks.TranslateTask
import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.Jar

/*
 * Main plugin class for creation of extension object and all the tasks.
 */
class DopplPlugin implements Plugin<Project> {

    public static final String TASK_DOPPL_DEPLOY_MAIN = 'dopplDeployMain'
    public static final String TASK_DOPPL_DEPLOY_TEST = 'dopplDeployTest'
    public static final String TASK_DOPPL_DEPLOY = 'dopplDeploy'

    public static final String TASK_DOPPL_ASSEMBLY = 'dopplAssembly'
    public static final String TASK_DOPPL_ARCHIVE = 'dopplArchive'

    @Override
    void apply(Project project) {

        DopplVersionManager.verifyJ2objcRequirements(project)

        boolean javaTypeProject = Utils.isJavaTypeProject(project);

        boolean androidTypeProject = Utils.isAndroidTypeProject(project);

        if(!javaTypeProject && !androidTypeProject) {
            throw new ProjectConfigurationException("Doppl depends on running java or one of the Android gradle plugins. None of those were found. If you have one, please make sure to apply doppl AFTER the other plugin(s)", null)
        }

        if(javaTypeProject) {
            project.configurations {
                provided {
                    dependencies.all { dep ->
                        project.configurations.default.exclude group: dep.group, module: dep.name
                    }
                }
                compile.extendsFrom provided
            }
        }

        project.with {
            extensions.create('dopplConfig', DopplConfig, project)

            BuildContext buildContext = new BuildContext(project)

            // This is an intermediate directory only.  Clients should use only directories
            // specified in dopplConfig (or associated defaults in dopplConfig).
            File j2objcSrcGenMainDir = file("${buildDir}/j2objcSrcGenMain")
            File j2objcSrcGenTestDir = file("${buildDir}/j2objcSrcGenTest")

            // These configurations are groups of artifacts and dependencies for the plugin build
            // https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
            configurations {
                doppl{
                    transitive = true
                    description = 'For doppl special packages'
                }
                testDoppl{
                    transitive = true
                    description = 'For doppl testing special packages'
                }
            }

            dependencies {
                if(javaTypeProject) {
                    provided project.files(Utils.j2objcHome(project) + "/lib/jre_emul.jar")
                }

                compile 'com.google.j2objc:j2objc-annotations:0.2.1'
            }

            // Produces a modest amount of output
            logging.captureStandardOutput LogLevel.INFO

            // If users need to generate extra files that j2objc depends on, they can make this task dependent
            // on such generation.

            Task prebuildTask = tasks.create(name: 'j2objcPreBuild', type: DefaultTask) {
                group 'doppl'
                description "Marker task for all tasks that must be complete before j2objc building"
            }

            //What runs before you run
            buildContext.getBuildTypeProvider().configureDependsOn(project, prebuildTask)

            tasks.create(name: 'j2objcTranslate', type: TranslateTask,
                    dependsOn: 'j2objcPreBuild') {
                group 'doppl'
                description "Translates all the java source files in to Objective-C using 'j2objc'"
                _buildContext = buildContext

                // Output directories of 'j2objcTranslate', input for all other tasks
                srcGenMainDir = j2objcSrcGenMainDir
                srcGenTestDir = j2objcSrcGenTestDir
                try {
                    //TODO: This should probably be more configurable
                    def file = file('src/main/objc')
                    if(file.exists())
                        srcMainObjcDir = file
                } catch (Exception e) {
                    //Ugh
                }
                try {
                    //TODO: This should probably be more configurable
                    def file = file('src/test/objc')
                    if(file.exists())
                        srcTestObjcDir = file
                } catch (Exception e) {
                    //Don't care
                }
            }

            tasks.create(name: TASK_DOPPL_DEPLOY_MAIN, type: DeployTask,
                    dependsOn: 'j2objcTranslate') {
                group 'doppl'
                description 'Push main code to Xcode directory (or wherever you want)'

                srcGenDir = j2objcSrcGenMainDir
                testCode = false
                _buildContext = buildContext
            }

            tasks.create(name: TASK_DOPPL_DEPLOY_TEST, type: DeployTask,
                    dependsOn: 'j2objcTranslate') {
                group 'doppl'
                description 'Push test code to Xcode directory (or wherever you want)'

                srcGenDir = j2objcSrcGenTestDir
                testCode = true
                _buildContext = buildContext
            }

            tasks.create(name: TASK_DOPPL_DEPLOY, type: DefaultTask, dependsOn: [
                    TASK_DOPPL_DEPLOY_MAIN,
                    TASK_DOPPL_DEPLOY_TEST
                    ]) {
                group 'doppl'
                description "Wrapper task to build and deploy translated objc to xcode directories"
            }

            //************** LIBRARY TASKS **************
            //The following tasks are geared towards library assembly and archiving

            tasks.create(name: TASK_DOPPL_ASSEMBLY, type: DopplAssemblyTask,
                    dependsOn: 'j2objcTranslate') {
                group 'doppl'
                description 'Pull together doppl pieces for library projects'

                srcGenMainDir = j2objcSrcGenMainDir
            }

            tasks.create(name: TASK_DOPPL_ARCHIVE, type: Jar, dependsOn: TASK_DOPPL_ASSEMBLY) {
                group 'doppl'
                description 'Depends on j2objc build, move all doppl stuff to deploy dir'

                from project.dopplConfig.destDopplFolder
                extension 'dop'
            }



            /*// j2objcCycleFinder must be run manually with ./gradlew j2objcCycleFinder
           tasks.create(name: 'j2objcCycleFinder', type: CycleFinderTask,
                   dependsOn: 'j2objcPreBuild') {
               group 'doppl'
               description "Run the cycle_finder tool on all Java source files"

               outputs.upToDateWhen { false }
           }*/
        }


    }
}
