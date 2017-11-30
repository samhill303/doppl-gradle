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

import co.touchlab.doppl.gradle.tasks.CycleFinderTask
import co.touchlab.doppl.gradle.tasks.DeployTask
import co.touchlab.doppl.gradle.tasks.DopplAssemblyTask
import co.touchlab.doppl.gradle.tasks.FrameworkTask
import co.touchlab.doppl.gradle.tasks.HeaderMappingsTask
import co.touchlab.doppl.gradle.tasks.JavaStagingTask
import co.touchlab.doppl.gradle.tasks.TestTranslateTask
import co.touchlab.doppl.gradle.tasks.TranslateDependenciesTask
import co.touchlab.doppl.gradle.tasks.TranslateTask
import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar

/*
 * Main plugin class for creation of extension object and all the tasks.
 */
class DopplPlugin implements Plugin<Project> {

    public static final String TASK_DOPPL_DEPLOY_MAIN = 'dopplDeployMain'
    public static final String TASK_DOPPL_DEPLOY_TEST = 'dopplDeployTest'
    public static final String TASK_DOPPL_DEPLOY = 'dopplDeploy'
    public static final String TASK_DOPPL_TEST_TRANSLATE = 'dopplTest'

    public static final String TASK_DOPPL_ASSEMBLY = 'dopplAssembly'
    public static final String TASK_DOPPL_ARCHIVE = 'dopplArchive'
    public static final String TASK_DOPPL_JAVA_STAGING_MAIN = 'dopplJavaStagingMain'
    public static final String TASK_DOPPL_JAVA_STAGING_TEST = 'dopplJavaStagingTest'
    public static final String TASK_DOPPL_DEPENDENCY_TRANSLATE_MAIN = 'dopplDependencyTranslateMain'
    public static final String TASK_DOPPL_DEPENDENCY_TRANSLATE_TEST = 'dopplDependencyTranslateTest'
    public static final String TASK_J2OBJC_MAIN_TRANSLATE = 'j2objcMainTranslate'
    public static final String TASK_J2OBJC_TEST_TRANSLATE = 'j2objcTestTranslate'
    public static final String TASK_DOPPL_FRAMEWORK_MAIN = 'dopplFrameworkMain'
    public static final String TASK_DOPPL_FRAMEWORK_TEST = 'dopplFrameworkTest'
    public static final String TASK_DOPPL_BUILD = 'dopplBuild'

    public static final String TASK_J2OBJC_CYCLE_FINDER = 'j2objcCycleFinder'

    public static final String FOLDER_J2OBJC_OUT_MAIN = 'j2objcSrcGenMain'
    public static final String FOLDER_J2OBJC_OUT_TEST = 'j2objcSrcGenTest'

    public static final String FOLDER_DOPPL_DEP = 'dopplDependency'
    public static final String FOLDER_DOPPL_DEP_EXPLODED = 'dopplDependencyExploded'
    public static final String FOLDER_DOPPL_ONLY_DEP_EXPLODED = 'dopplOnlyDependencyExploded'
    public static final String FOLDER_TEST_DOPPL_DEP_EXPLODED = 'testDopplDependencyExploded'

    public static final String DOPPL_DEPENDENCY_RESOLVER = 'dopplDependencyResolver'

    public static final String DOPPL_JAVA_MAIN = 'dopplJavaMain'
    public static final String DOPPL_JAVA_TEST = 'dopplJavaTest'
    public static final String TASK_J2OBJC_PRE_BUILD = 'j2objcPreBuild'
    public static final String TASK_DOPPL_CONTEXT_BUILD = 'dopplContextBuild'
    public static final String TASK_DOPPL_HEADER_MAPPINGS = "dopplHeaderMappings"

    @Override
    void apply(Project project) {

        DopplVersionManager.verifyJ2objcRequirements(project)

        boolean javaTypeProject = Utils.isJavaTypeProject(project);

        boolean androidTypeProject = Utils.isAndroidTypeProject(project);

        if(!javaTypeProject && !androidTypeProject) {
            throw new ProjectConfigurationException("Doppl depends on running java or one of the Android gradle plugins. None of those were found. If you have one, please make sure to apply doppl AFTER the other plugin(s)", null)
        }

        project.with {
            extensions.create('dopplConfig', DopplConfig, project)

            extensions.dopplConfig.extensions.create('mainFramework', FrameworkConfig, false)
            extensions.dopplConfig.extensions.create('testFramework', FrameworkConfig, true)

            // This is an intermediate directory only.  Clients should use only directories
            // specified in dopplConfig (or associated defaults in dopplConfig).
            File j2objcSrcGenMainDir = file("${buildDir}/${FOLDER_J2OBJC_OUT_MAIN}")
            File j2objcSrcGenTestDir = file("${buildDir}/${FOLDER_J2OBJC_OUT_TEST}")

            // These configurations are groups of artifacts and dependencies for the plugin build
            // https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
            configurations {
                doppl{
                    transitive = true
                    description = 'For doppl special packages'
                }
                dopplOnly{
                    transitive = true
                    description = 'For doppl special packages, do not include in dependencies'
                }
                testDoppl{
                    transitive = true
                    description = 'For doppl testing special packages'
                }
            }

            if(Utils.j2objcHomeOrNull(project) != null) {
                dependencies {
                    if (javaTypeProject) {
                        compileOnly project.files(Utils.j2objcHome(project) + "/lib/jre_emul.jar")
                        testCompile project.files(Utils.j2objcHome(project) + "/lib/jre_emul.jar")
                        compileOnly project.files(Utils.j2objcHome(project) + "/lib/j2objc_annotations.jar")
                        testCompile project.files(Utils.j2objcHome(project) + "/lib/j2objc_annotations.jar")
                    } else {
                        compile project.files(Utils.j2objcHome(project) + "/lib/j2objc_annotations.jar")
                    }
                }
            }

            // Produces a modest amount of output
            logging.captureStandardOutput LogLevel.INFO

            // If users need to generate extra files that j2objc depends on, they can make this task dependent
            // on such generation.

            DependencyResolver dependencyResolver = (DependencyResolver)tasks.create(name: DOPPL_DEPENDENCY_RESOLVER, type: DependencyResolver)

            BuildContext buildContext = new BuildContext(project, dependencyResolver)

            Task j2objcPreBuildTask = tasks.create(name: TASK_J2OBJC_PRE_BUILD, type: DefaultTask, dependsOn: DOPPL_DEPENDENCY_RESOLVER) {
                group 'doppl'
                description "Marker task for all tasks that must be complete before j2objc building"
            }

            Task dopplContextBuildTask = tasks.create(name: TASK_DOPPL_CONTEXT_BUILD, type: DefaultTask, dependsOn: TASK_J2OBJC_PRE_BUILD) {
                group 'doppl'
                description "Marker task for all tasks after the underlying Java build system runs"
            }

            Task mainStagingTask = tasks.create(name: TASK_DOPPL_JAVA_STAGING_MAIN, type: DefaultTask) {
                group 'doppl'
                description "Umbrella for main staging tasks"
            }

            Task testStagingTask = tasks.create(name: TASK_DOPPL_JAVA_STAGING_TEST, type: DefaultTask) {
                group 'doppl'
                description "Umbrella for test staging tasks"
            }

            tasks.create(name: TASK_J2OBJC_MAIN_TRANSLATE, type: TranslateTask,
                    dependsOn: [TASK_DOPPL_DEPENDENCY_TRANSLATE_MAIN, TASK_DOPPL_JAVA_STAGING_MAIN]) {
                group 'doppl'
                description "Translates main java source files to Objective-C"
                _buildContext = buildContext

                // Output directories of 'j2objcTranslate', input for all other tasks
                srcGenDir = j2objcSrcGenMainDir
            }

            tasks.create(name: TASK_J2OBJC_TEST_TRANSLATE, type: TranslateTask,
                    dependsOn: [TASK_DOPPL_DEPENDENCY_TRANSLATE_TEST, TASK_DOPPL_JAVA_STAGING_TEST]) {
                group 'doppl'
                description "Translates test java source files to Objective-C"
                _buildContext = buildContext

                // Output directories of 'j2objcTranslate', input for all other tasks
                srcGenDir = j2objcSrcGenTestDir
                testBuild = true
            }

            TranslateDependenciesTask mainDepJar = (TranslateDependenciesTask)tasks.create(name: TASK_DOPPL_DEPENDENCY_TRANSLATE_MAIN, type: TranslateDependenciesTask, dependsOn: TASK_J2OBJC_PRE_BUILD){
                _buildContext = buildContext
                testBuild = false
            }

            TranslateDependenciesTask testDepJar = (TranslateDependenciesTask)tasks.create(name: TASK_DOPPL_DEPENDENCY_TRANSLATE_TEST, type: TranslateDependenciesTask, dependsOn: TASK_J2OBJC_PRE_BUILD){
                _buildContext = buildContext
                testBuild = true
            }

            afterEvaluate {

                dependencyResolver.configureAll()

                mainDepJar.initJarTasks(project, tasks, j2objcPreBuildTask)
                testDepJar.initJarTasks(project, tasks, j2objcPreBuildTask)

                prepStagingTasks(
                        tasks,
                        buildContext.getBuildTypeProvider().sourceSets(project),
                        TASK_DOPPL_JAVA_STAGING_MAIN,
                        DopplConfig.from(project).getDopplJavaDirFileMain(),
                        mainStagingTask,
                        dopplContextBuildTask
                )

                if(!DopplConfig.from(project).skipTests) {
                    prepStagingTasks(
                            tasks,
                            buildContext.getBuildTypeProvider().testSourceSets(project),
                            TASK_DOPPL_JAVA_STAGING_TEST,
                            DopplConfig.from(project).getDopplJavaDirFileTest(),
                            testStagingTask,
                            dopplContextBuildTask
                    )
                }
                buildContext.getBuildTypeProvider().configureDependsOn(project, j2objcPreBuildTask, dopplContextBuildTask)
                buildContext.getBuildTypeProvider().configureTestDependsOn(project, j2objcPreBuildTask, dopplContextBuildTask)
            }

            tasks.create(name: TASK_DOPPL_ASSEMBLY, type: DopplAssemblyTask,
                    dependsOn: TASK_J2OBJC_MAIN_TRANSLATE) {
                group 'doppl'
                description 'Pull together doppl pieces for library projects'

                try {
                    //TODO: This should probably be more configurable
                    def file = file('src/main/objc')
                    if (file.exists())
                        srcObjcDir = file
                } catch (Exception e) {
                    //Ugh
                }

                dopplJavaDirFile = DopplConfig.from(project).getDopplJavaDirFileMain()
                srcGenMainDir = j2objcSrcGenMainDir
            }



            tasks.create(name: TASK_DOPPL_TEST_TRANSLATE, type: TestTranslateTask,
                    dependsOn: TASK_J2OBJC_TEST_TRANSLATE) {
                group 'doppl'
                description "Compiles a list of the test classes in your project"
                _buildContext = buildContext

                output = file("$j2objcSrcGenTestDir/dopplTests.txt")
            }

            tasks.create(name: TASK_DOPPL_FRAMEWORK_MAIN, type: FrameworkTask,
                    dependsOn: [TASK_DOPPL_ASSEMBLY, TASK_DOPPL_DEPENDENCY_TRANSLATE_MAIN]) {
                group 'doppl'
                description 'Create framework podspec'
                _buildContext = buildContext
                test = false
            }

            tasks.create(name: TASK_DOPPL_FRAMEWORK_TEST, type: FrameworkTask,
                    dependsOn: [TASK_DOPPL_TEST_TRANSLATE, TASK_DOPPL_DEPENDENCY_TRANSLATE_TEST]) {
                group 'doppl'
                description 'Create framework podspec'
                _buildContext = buildContext
                test = true
            }

            tasks.create(name: TASK_DOPPL_BUILD, type: DefaultTask,
                    dependsOn: [TASK_DOPPL_FRAMEWORK_MAIN, TASK_DOPPL_FRAMEWORK_TEST]) {
                group 'doppl'
                description 'Build doppl'
            }

            tasks.create(name: TASK_DOPPL_DEPLOY_MAIN, type: DeployTask,
                    dependsOn: TASK_DOPPL_ASSEMBLY) {
                group 'doppl'
                description 'Push main code to Xcode directory (or wherever you want)'

                srcGenDir = j2objcSrcGenMainDir
                testCode = false
                _buildContext = buildContext
            }

            tasks.create(name: TASK_DOPPL_DEPLOY_TEST, type: DeployTask, dependsOn: TASK_DOPPL_TEST_TRANSLATE) {
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

            tasks.create(name: TASK_DOPPL_ARCHIVE, type: Jar, dependsOn: TASK_DOPPL_ASSEMBLY) {
                group 'doppl'
                description 'Depends on j2objc build, move all doppl stuff to deploy dir'

                from project.dopplConfig.destDopplFolder
                extension 'dop'
            }

            //Clean all the things
            tasks.findByName("clean").doFirst {
                DopplConfig dopplConfig = DopplConfig.from(project)
                project.delete(dopplConfig.copyMainOutput)
                project.delete(dopplConfig.copyTestOutput)
            }

            // j2objcCycleFinder must be run manually with ./gradlew j2objcCycleFinder
           tasks.create(name: TASK_J2OBJC_CYCLE_FINDER, type: CycleFinderTask,
                   dependsOn: TASK_DOPPL_JAVA_STAGING_MAIN) {
               group 'doppl'
               description "Run the cycle_finder tool on all Java source files"

               dopplJavaDirFile = DopplConfig.from(project).getDopplJavaDirFileMain()
               _buildContext = buildContext
           }

            //Convenience task. Will probably go away.
            tasks.create(name: TASK_DOPPL_HEADER_MAPPINGS, type: HeaderMappingsTask, dependsOn: TASK_DOPPL_JAVA_STAGING_MAIN)
        }


    }

    void prepStagingTasks(TaskContainer tasks, List<FileTree> sets, String wrapperTaskName, File dest, Task wrapperTask, Task upstreamTask)
    {
        int count = 0
        for (FileTree tree : sets) {
            Task t = tasks.create(
                    name: wrapperTaskName + "_" + (count++),
                    type: JavaStagingTask,
                    description: "JavaStagingTask ${tree.toString()}") {
                sourceFileTree = tree
                destDir = dest
            }
            wrapperTask.dependsOn t
            t.dependsOn(upstreamTask)
        }
    }
}
