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
import co.touchlab.doppl.gradle.BuildTypeProvider
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import co.touchlab.doppl.gradle.DopplInfo
import co.touchlab.doppl.gradle.analytics.DopplAnalytics
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil

/**
 * Translation task for Java to Objective-C using j2objc tool.
 */
@CompileStatic
class TranslateTask extends BaseChangesTask {

    boolean testBuild

    @InputFiles
    FileCollection getInputFiles()
    {
        FileTree fileTree = new UnionFileTree("TranslateTask - ${(testBuild ? "test" : "main")}")

        BuildTypeProvider buildTypeProvider = _buildContext.getBuildTypeProvider()
        List<FileTree> sets = testBuild ? buildTypeProvider.testSourceSets(project) : buildTypeProvider.sourceSets(project)
        for (FileTree set : sets) {
            fileTree.add(set)
        }

        DopplConfig dopplConfig = DopplConfig.from(project)
        if(dopplConfig.translatePattern != null) {
            fileTree = fileTree.matching(dopplConfig.translatePattern)
        }

        fileTree = fileTree.matching(javaPattern {
            include "**/*.java"
        })

        return fileTree
    }

    static PatternSet javaPattern(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        PatternSet translatePattern = new PatternSet()
        return ConfigureUtil.configure(cl, translatePattern)
    }

    File getMappingsFile()
    {
        DopplInfo dopplInfo = DopplInfo.getInstance(project)
        if(testBuild)
            return dopplInfo.sourceBuildOutTestMappings()
        else
            return dopplInfo.sourceBuildOutMainMappings()
    }

    @Input
    String getDependencyList()
    {
        return PodManagerTask.getDependencyList(_buildContext, testBuild)
    }

    @Input boolean isEmitLineDirectives() {
        DopplConfig.from(project).emitLineDirectives
    }

    @InputDirectory @Optional
    File getObjcDir(){
        File f = testBuild ? project.file(DopplInfo.SOURCEPATH_OBJC_TEST) : project.file(DopplInfo.SOURCEPATH_OBJC_MAIN)
        return f.exists() ? f : null
    }

    @OutputDirectory
    File getBuildOut() {
        if(testBuild)
            return DopplInfo.getInstance(project).sourceBuildOutFileTest()
        else
            return DopplInfo.getInstance(project).sourceBuildOutFileMain()
    }



    @TaskAction
    void translate(IncrementalTaskInputs inputs) {

        DopplConfig dopplConfig = DopplConfig.from(project)
        if(testBuild && dopplConfig.skipTests)
            return

        if(!dopplConfig.disableAnalytics) {
            new DopplAnalytics(dopplConfig, Utils.findVersionString(project, Utils.j2objcHome(project))).execute()
        }

        File objcDir = getObjcDir()
        if(objcDir != null)
        {
            Utils.projectCopy(project, {
                from objcDir
                into getBuildOut()
                includeEmptyDirs = false
            })
        }

        List<String> translateArgs = getTranslateArgs()

        def prefixMap = getPrefixes()

        doTranslate(
                allJavaFolders(),
                translateArgs,
                prefixMap,
                isEmitLineDirectives()
        )

        if (prefixMap.size() > 0) {
            def prefixes = new File(project.buildDir, "prefixes.properties")
            def writer = new FileWriter(prefixes)

            Utils.propsFromStringMap(prefixMap).store(writer, null);

            writer.close()
        }
    }

    void recursiveGrab(File dir, List<File> files)
    {
        if(dir.isDirectory())
        {
            File[] dirFiles = dir.listFiles()
            for (File f : dirFiles) {
                if(f.isDirectory())
                    recursiveGrab(f, files)
                else if(f.getName().endsWith(".java"))
                    files.add(f);
            }
        }
    }

    void doTranslate(
            Collection<File> sourcepathDirs,
            List<String> translateArgs,
            Map<String, String> prefixMap,
            boolean emitLineDirectives) {

        DopplInfo dopplInfo = DopplInfo.getInstance(project)
        String j2objcExecutable = "${getJ2objcHome()}/j2objc"

        String sourcepathArg = Utils.joinedPathArg(sourcepathDirs)

        List<DopplDependency> dopplLibs = getTranslateDopplLibs(_buildContext, testBuild)

        //Classpath arg for translation. Includes user specified jars, j2objc 'standard' jars, and doppl dependency libs
        UnionFileCollection classpathFiles = new UnionFileCollection([
                project.files(Utils.j2objcLibs(getJ2objcHome(), getTranslateJ2objcLibs()))
        ])

        String classpathArg = Utils.joinedPathArg(classpathFiles)

        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        List<String> mappingFiles = new ArrayList<>()
        Map<String, String> allPrefixes = new HashMap<>(prefixMap)

        addMappings(dopplInfo.dependencyOutMainMappings(), mappingFiles)

        if(testBuild)
        {
            addMappings(dopplInfo.dependencyOutTestMappings(), mappingFiles)
            addMappings(dopplInfo.sourceBuildOutMainMappings(), mappingFiles)
        }

        File buildOut = getBuildOut()
        buildOut.mkdirs()
        File javaBatch = new File(buildOut, "javabatch.in")

        javaBatch.write(getInputFiles().files.join("\n"))

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable

                args "-d", Utils.relativePath(project.projectDir, buildOut)
                args "-XcombineJars", ''
                args "-XglobalCombinedOutput", "${testBuild ? 'test' : 'main'}SourceOut"
                args "--swift-friendly", ''
                args "--output-header-mapping", getMappingsFile().path

                if(emitLineDirectives)
                {
                    args "-g", ''
                }

                if (mappingFiles.size() > 0) {
                    args "--header-mapping", mappingFiles.join(",")
                }

                args "-sourcepath", sourcepathArg
                args "-classpath", classpathArg
                translateArgs.each { String translateArg ->
                    args translateArg
                }

                allPrefixes.keySet().each { String packageString ->
                    args "--prefix", packageString + "=" + allPrefixes.get(packageString)
                }

                args "@${Utils.relativePath(project.projectDir, javaBatch)}"

                setStandardOutput stdout
                setErrorOutput stderr

                setWorkingDir project.projectDir
            })

        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
        }

        javaBatch.delete()
    }

    private void addMappings(File mapFile, List<String> mappingFiles) {
        if(mapFile.exists())
            mappingFiles.add(mapFile.path)
    }

    static List<DopplDependency> getTranslateDopplLibs(BuildContext _buildContext, boolean testBuild) {
        List<DopplDependency> libs = new ArrayList<>()
        libs.addAll(_buildContext.getDependencyResolver().translateDopplLibs)
        if(testBuild)
        {
            libs.addAll(_buildContext.getDependencyResolver().translateDopplTestLibs)
        }
        return libs
    }

    static Set<File> depJavaFolders(BuildContext _buildContext, boolean testBuild)
    {
        List<DopplDependency> dopplLibs = getTranslateDopplLibs(_buildContext, testBuild)

        return depLibsToJavaFolders(dopplLibs)
    }

    static Set<File> depLibsToJavaFolders(List<DopplDependency> dopplLibs) {
        Set<File> javaFolders = new HashSet<>()
        for (DopplDependency dependency : dopplLibs) {
            File javaFolder = dependency.dependencyJavaFolder()
            if (javaFolder.exists())
                javaFolders.add(javaFolder)
        }

        return javaFolders
    }

    static Set<File> allJavaFolders(Project project, BuildContext _buildContext, boolean testBuild)
    {
        Set<File> allFiles = new HashSet<>()
        allFiles.addAll(depJavaFolders(_buildContext, testBuild))

        allFiles.addAll(mainSourceDirs(project, _buildContext))
        if(testBuild){
            allFiles.addAll(testSourceDirs(project, _buildContext))
        }
        return allFiles
    }

    static Set<File> mainSourceDirs(Project project, BuildContext _buildContext)
    {
        Set<File> files = new HashSet<>()
        fillDirsFromTrees(_buildContext.getBuildTypeProvider().sourceSets(project), files)
        return files
    }

    static Set<File> testSourceDirs(Project project, BuildContext _buildContext)
    {
        Set<File> files = new HashSet<>()
        fillDirsFromTrees(_buildContext.getBuildTypeProvider().testSourceSets(project), files)
        return files
    }

    private static void fillDirsFromTrees(List<FileTree> mainSourceSets, Set<File> allFiles) {
        for (FileTree fileTree : mainSourceSets) {
            allFiles.add(Utils.dirFromFileTree(fileTree))
        }
    }

    //All the java source dirs we're going to try to translate
    Set<File> allJavaFolders() {
        return allJavaFolders(project, _buildContext, testBuild)
    }
}
