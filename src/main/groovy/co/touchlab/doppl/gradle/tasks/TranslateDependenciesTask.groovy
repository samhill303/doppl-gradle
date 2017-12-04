/*
 * Copyright (c) 2017 Touchlab Inc
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

package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.BuildContext
import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import co.touchlab.doppl.gradle.DopplInfo
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class TranslateDependenciesTask extends BaseChangesTask{

    BuildContext _buildContext
    boolean testBuild

    List<File> allJars = new ArrayList<>()

    @Input
    String getDependencyVersions()
    {
        DependencyResolver resolver = _buildContext.getDependencyResolver()
        List<DopplDependency> libs = dependencyList(resolver)

        return flattenLibs(libs)
    }

    @OutputDirectory
    File getBuildJarOut() {
        return getBuildJarOut(project, testBuild)
    }

    static File getBuildJarOut(Project project, boolean testBuild)
    {
        return DopplInfo.getInstance(project).dependencyBuildJarFileForPhase(testBuild ? "test" : "main")
    }

    static File getObjcOutDir(Project project)
    {
        return DopplInfo.getInstance(project).dependencyExplodedFile()
    }

    Map<String, String> getPrefixes() {
        return DopplConfig.from(project).translatedPathPrefix
    }

    private List<DopplDependency> dependencyList(DependencyResolver resolver) {
        return testBuild ? resolver.translateDopplTestLibs : resolver.translateDopplLibs
    }

    public void initJarTasks(Project project, TaskContainer tasks, Task forwardDependes)
    {
        List<DopplDependency> dependencies = dependencyList(_buildContext.getDependencyResolver())
        int count = 0
        String phase = testBuild ? "test" : "main"
        File dependencySourceJarFolder = DopplInfo.getInstance(project).dependencyBuildJarFileForPhase(phase)

        for (DopplDependency dep : dependencies) {
            String taskName = "depJar_" + phase + "_" + (count++)
            String jarName = taskName + ".jar"

            Task t = tasks.create(name: taskName, type: Jar){
                from dep.dependencyJavaFolder()
                destinationDir = dependencySourceJarFolder
                archiveName = jarName
            }

            allJars.add(new File(dependencySourceJarFolder, jarName))

            this.dependsOn(t)

            t.dependsOn(forwardDependes)
        }
    }

    //TODO: This assumes the folders are distinct. Need a better solution.
    private String flattenLibs(List<DopplDependency> libs)
    {
        List<String> parts = new ArrayList<>()
        for (DopplDependency dep : libs) {
            parts.add(dep.dependencyFolderLocation().name)
        }

        return parts.join("|")
    }

    String getJ2objcHome() { return Utils.j2objcHome(project) }

    List<String> getTranslateJ2objcLibs() { return DopplConfig.from(project).translateJ2objcLibs }

    List<String> getTranslateArgs() {
        return DopplConfig.from(project).processedTranslateArgs()
    }

    static File dependencyMappingsFile(Project project, boolean test)
    {
        return new File(getObjcOutDir(project), "doppl_${test ? "test" : "main"}.mappings")
    }

    @TaskAction
    void translateDependencies(IncrementalTaskInputs inputs) {

        String j2objcExecutable = "${getJ2objcHome()}/j2objc"

        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        if (allJars.size() == 0)
            return

        UnionFileCollection classpathFiles = new UnionFileCollection([
                project.files(Utils.j2objcLibs(getJ2objcHome(), getTranslateJ2objcLibs()))
        ])

        String classpathArg = Utils.joinedPathArg(classpathFiles) +
                              File.pathSeparator + "${project.buildDir}/classes"

        List<File> sourcepathList = new ArrayList<>()

        if (testBuild) {
            ArrayList<DopplDependency> libs = _buildContext.getDependencyResolver().translateDopplLibs
            for (DopplDependency dep : libs) {
                sourcepathList.add(dep.dependencyJavaFolder())
            }
        }

        Map<String, String> allPrefixes = getPrefixes()

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable

                // Arguments
                args "-XcombineJars", ''
                args "-XupzipJarDir", DopplInfo.JAVA_SOURCE
                args "--swift-friendly", ''
                args "--output-header-mapping", dependencyMappingsFile(project, testBuild).absolutePath

                if(isDependenciesEmitLineDirectives())
                {
                    args "-g", ''
                }

                if (sourcepathList.size() > 0) {
                    args "-sourcepath", Utils.joinedPathArg(sourcepathList)
                }

                if(testBuild){
                    args "--header-mapping", dependencyMappingsFile(project, false).path
                }

                args "-classpath", classpathArg
                getTranslateArgs().each { String translateArg ->
                    args translateArg
                }

                allPrefixes.keySet().each { String packageString ->
                    args "--prefix", packageString + "=" + allPrefixes.get(packageString)
                }

                allJars.each { File f ->
                    args f.getName()
                }

                setStandardOutput stdout
                setErrorOutput stderr

                setWorkingDir getBuildJarOut()
            })

        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
        }
    }
}
