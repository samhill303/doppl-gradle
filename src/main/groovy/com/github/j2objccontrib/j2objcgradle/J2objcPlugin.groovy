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
import com.github.j2objccontrib.j2objcgradle.tasks.DoppelAssemblyTask
import com.github.j2objccontrib.j2objcgradle.tasks.TranslateTask
import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.Jar

/*
 * Main plugin class for creation of extension object and all the tasks.
 */
class J2objcPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        boolean javaTypeProject = project.plugins.hasPlugin('java')

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

        String version = BuildInfo.VERSION
        String commit = BuildInfo.GIT_COMMIT
        String url = BuildInfo.URL
        String timestamp = BuildInfo.TIMESTAMP

        project.logger.info("doppl-gradle plugin: Version $version, Built: $timestamp, Commit: $commit, URL: $url")

        // This avoids a lot of "project." prefixes, such as "project.tasks.create"
        project.with {
//            Utils.throwIfNoJavaPlugin(project)

            extensions.create('j2objcConfig', J2objcConfig, project)

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
                testDoppel{
                    transitive = true
                    description = 'For doppel testing special packages'
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

            if(javaTypeProject) {
                prebuildTask.dependsOn('jar')
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

            tasks.create(name: 'doppelAssembly', type: DoppelAssemblyTask,
                    dependsOn: 'j2objcTranslate') {
                group 'doppl'
                description 'Pull together doppel pieces'
                srcGenMainDir = j2objcSrcGenMainDir
            }

            tasks.create(name: 'doppelArchive', type: Jar, dependsOn: 'doppelAssembly') {
                group 'doppl'
                description 'Depends on j2objc build, move all doppel stuff to deploy dir'


                from project.j2objcConfig.destDoppelFolder
                extension 'dop'
            }

//            lateDependsOn(project, 'build', 'doppelArchive')
        }
    }

    // Has task named afterTaskName depend on the task named beforeTaskName, regardless of
    // whether afterTaskName has been created yet or not.
    // The before task must already exist.
    private static void lateDependsOn(Project proj, String afterTaskName, String beforeTaskName) {
        assert null != proj.tasks.findByName(beforeTaskName)
        // You can't just call tasks.findByName on afterTaskName - for certain tasks like 'assemble' for
        // reasons unknown, the Java plugin creates - right there! - the task; this prevents
        // later code from modifying binaries, sourceSets, etc.  If you see an error
        // mentioning 'state GraphClosed' saying you can't mutate some object, see if you are magically
        // causing Gradle to make the task by using findByName!  Issue #156

        // tasks.all cleanly calls this closure on any existing elements and for all elements
        // added in the future.
        // TODO: Find a better way to have afterTask depend on beforeTask, without
        // materializing afterTask early.
        proj.tasks.all { Task task ->
            if (task.name == afterTaskName) {
                task.dependsOn beforeTaskName
            }
        }
    }
}
