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

import com.github.j2objccontrib.j2objcgradle.tasks.CycleFinderTask
import com.github.j2objccontrib.j2objcgradle.tasks.TranslateTask
import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin

/*
 * Main plugin class for creation of extension object and all the tasks.
 */
class J2objcPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        String version = BuildInfo.VERSION
        String commit = BuildInfo.GIT_COMMIT
        String url = BuildInfo.URL
        String timestamp = BuildInfo.TIMESTAMP

        project.logger.info("doppl-gradle plugin: Version $version, Built: $timestamp, Commit: $commit, URL: $url")

        // This avoids a lot of "project." prefixes, such as "project.tasks.create"
        project.with {
            getPluginManager().apply(JavaPlugin)

            extensions.create('j2objcConfig', J2objcConfig, project)

            afterEvaluate { Project evaluatedProject ->
                if (!evaluatedProject.j2objcConfig.isFinalConfigured()) {
                    logger.error("Project '${evaluatedProject.name}' is missing finalConfigure():\n" +
                                 "https://github.com/j2objc-contrib/j2objc-gradle/blob/master/FAQ.md#how-do-i-call-finalconfigure")
                }

                boolean arcTranslateArg = true;// = '-use-arc' in evaluatedProject.j2objcConfig.translateArgs
                boolean arcCompilerArg = true;//'-fobjc-arc' in evaluatedProject.j2objcConfig.extraObjcCompilerArgs
                if (arcTranslateArg && !arcCompilerArg || !arcTranslateArg && arcCompilerArg) {
                    logger.error("Project '${evaluatedProject.name}' is missing required ARC flags:\n" +
                                 "https://github.com/j2objc-contrib/j2objc-gradle/blob/master/FAQ.md#how-do-i-enable-arc-for-my-translated-objective-c-classes")
                }
            }

            // This is an intermediate directory only.  Clients should use only directories
            // specified in j2objcConfig (or associated defaults in J2objcConfig).
            File j2objcSrcGenMainDir = file("${buildDir}/j2objcSrcGenMain")
            File j2objcSrcGenTestDir = file("${buildDir}/j2objcSrcGenTest")

            // These configurations are groups of artifacts and dependencies for the plugin build
            // https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
            configurations {
                doppel{
                    transitive = true
                    description = 'For doppel special packages'
                }
            }

            dependencies {

                provided project.files(Utils.j2objcHome(project) + "/lib/jre_emul.jar")
/*
                if(!project.name.equals('androidbase') &&
                   !project.name.equals('androidbasetest') &&
                   !project.name.equals('testapt'))
                {
                    provided 'co.touchlab.doppel:androidbase:0.4.0-SNAPSHOT'
                    doppel 'co.touchlab.doppel:androidbase:0.4.0-SNAPSHOT@dop'
                    testCompile 'co.touchlab.doppel:androidbasetest:0.4.0-SNAPSHOT'
                    doppel 'co.touchlab.doppel:androidbasetest:0.4.0-SNAPSHOT@dop'
                }*/

                compile 'com.google.j2objc:j2objc-annotations:0.2.1'
            }

            // Produces a modest amount of output
            logging.captureStandardOutput LogLevel.INFO

            // If users need to generate extra files that j2objc depends on, they can make this task dependent
            // on such generation.
            tasks.create(name: 'j2objcPreBuild', type: DefaultTask, dependsOn: 'build') {
                group 'doppl'
                description "Marker task for all tasks that must be complete before j2objc building"
            }

            // TODO @Bruno "build/source/apt" must be project.j2objcConfig.generatedSourceDirs no idea how to set it
            // there
            // Dependency may be added in project.plugins.withType for Java or Android plugin
            tasks.create(name: 'j2objcTranslate', type: TranslateTask,
                    dependsOn: 'j2objcPreBuild') {
                group 'doppl'
                description "Translates all the java source files in to Objective-C using 'j2objc'"
                // Output directories of 'j2objcTranslate', input for all other tasks
                srcGenMainDir = j2objcSrcGenMainDir
                srcGenTestDir = j2objcSrcGenTestDir
                try {
                    def file = file('src/main/objc')
                    if(file.exists())
                        srcMainObjcDir = file
                } catch (Exception e) {
                    //Ugh
                }
                try {
                    def file = file('src/test/objc')
                    if(file.exists())
                        srcTestObjcDir = file
                } catch (Exception e) {
                    //Don't care
                }
            }

            // j2objcCycleFinder must be run manually with ./gradlew j2objcCycleFinder
            tasks.create(name: 'j2objcCycleFinder', type: CycleFinderTask,
                    dependsOn: 'j2objcPreBuild') {
                group 'doppl'
                description "Run the cycle_finder tool on all Java source files"
                outputs.upToDateWhen { false }
            }

            /*tasks.create(name: 'j2objcAssembleResources', type: AssembleResourcesTask,
                    dependsOn: ['j2objcPreBuild']) {
                group 'doppl'
                description 'Copies mains and test resources to assembly directories'
            }

            tasks.create(name: 'j2objcAssembleSource', type: AssembleSourceTask,
                    dependsOn: ['j2objcTranslate']) {
                group 'doppl'
                description 'Copies final generated source to assembly directories'
                srcGenMainDir = j2objcSrcGenMainDir
                srcGenTestDir = j2objcSrcGenTestDir
            }*/

/*            tasks.create(name: 'j2objcPodspec', type: PodspecTask,
                    dependsOn: ['j2objcPreBuild']) {
                // podspec may reference resources that haven't yet been built
                group 'doppl'
                description 'Generate debug and release podspec that may be used for Xcode'
            }*/

            /*tasks.create(name: 'doppelArchive', type: Jar, dependsOn: 'doppelAssembly') {
                group 'doppl'
                description 'Depends on j2objc build, move all doppel stuff to deploy dir'

                from project.j2objcConfig.destDoppelFolder
                extension 'dop'
            }*/

            /*tasks.create(name: 'j2objcXcode', type: XcodeTask,
                    dependsOn: 'doppelArchive') {
                // pod install is ok when podspec references resources that haven't yet been built
                group 'doppl'
                description 'Depends on j2objc translation, create a Pod file link it to Xcode project'
            }*/
        }
    }
}
