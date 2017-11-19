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
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import co.touchlab.doppl.gradle.analytics.DopplAnalytics
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Translation task for Java to Objective-C using j2objc tool.
 */
class TranslateTask extends BaseChangesTask {

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

        DopplConfig dopplConfig = DopplConfig.from(project)
        allFiles.add(dopplConfig.getDopplJavaDirFileMain())
        if(testBuild){
            allFiles.add(dopplConfig.getDopplJavaDirFileTest())
        }
        return allFiles
    }

    //All the java source dirs we're going to try to translate
    Set<File> allJavaFolders() {
        return allJavaFolders(project, _buildContext, testBuild)
    }

    @InputFiles
    FileCollection allJavaFiles() {
        DopplConfig dopplConfig = DopplConfig.from(project)

        Set<File> folders = new HashSet<>()

        if(testBuild)
        {
            folders.addAll(depLibsToJavaFolders(_buildContext.getDependencyResolver().translateDopplTestLibs))
            folders.add(dopplConfig.getDopplJavaDirFileTest())
        }
        else
        {
            folders.addAll(depLibsToJavaFolders(_buildContext.getDependencyResolver().translateDopplLibs))
            folders.add(dopplConfig.getDopplJavaDirFileMain())
        }

        List<FileTree> fileTrees = new ArrayList<>(folders.size())

        for (File depFolder : folders) {
            fileTrees.add(project.fileTree(dir: depFolder, includes: ["**/*.java"]))
        }

        return new UnionFileTree("asdf", (Collection<? extends FileTree>)fileTrees)//.matching(dopplConfig.translatePattern)
    }

    @Input
    Map<String, String> getTranslateSourceMapping() { return DopplConfig.from(project).translateSourceMapping }

    // Generated ObjC files
    @OutputDirectory
    File srcGenDir

    boolean testBuild



    @TaskAction
    void translate(IncrementalTaskInputs inputs) {

        DopplConfig dopplConfig = DopplConfig.from(project)

        if(!dopplConfig.disableAnalytics) {
            new DopplAnalytics(dopplConfig, Utils.findVersionString(project, Utils.j2objcHome(project))).execute()
        }

        List<String> translateArgs = getTranslateArgs()

        // Don't evaluate this expensive property multiple times.
        FileCollection originalSrcFiles = allJavaFiles()

        FileCollection srcFilesChanged

        boolean forceFullBuild = !inputs.incremental
        srcFilesChanged = project.files()

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails details) {

                if(forceFullBuild)
                    return

                // We must filter by srcFiles, since all possible input files are @InputFiles to this task.
                if (originalSrcFiles.contains(details.file)) {
                    srcFilesChanged += project.files(details.file)
                } else {
                    forceFullBuild = true
                }
            }
        })

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            void execute(InputFileDetails details) {
                forceFullBuild = true
            }
        })

        println("forceFullBuild: "+ forceFullBuild + "/inputs.incremental: "+ inputs.incremental)

        if (forceFullBuild) {
            // A change outside of the source set directories has occurred, so an incremental build isn't possible.
            // The most common such change is in the JAR for a dependent library, for example if Java project
            // that this project depends on had its source changed and was recompiled.
            Utils.projectClearDir(project, srcGenDir)
            srcFilesChanged = originalSrcFiles
        }

        def prefixMap = getPrefixes()

        doTranslate(
                allJavaFolders(),
                srcGenDir,
                translateArgs,
                prefixMap,
                srcFilesChanged,
                "mainSrcFilesArgFile",
                false,
                isEmitLineDirectives()
        )

        Utils.projectCopy(project, {
            from originalSrcFiles
            into srcGenDir
            setIncludeEmptyDirs(false)
            include '**/*.mappings'
        })

        if (prefixMap.size() > 0) {
            def prefixes = new File(srcGenDir, "prefixes.properties")
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
            File srcDir,
            List<String> translateArgs,
            Map<String, String> prefixMap,
            FileCollection srcFilesToTranslate,
            String srcFilesArgFilename,
            boolean testTranslate,
            boolean emitLineDirectives) {

        Set<File> files = srcFilesToTranslate.getFiles()
        int num = files.size()
        logger.info("Translating $num files with j2objc...")
        if (files.size() == 0) {
            logger.info("No files to translate; skipping j2objc execution")
            return
        }

        String j2objcExecutable = "${getJ2objcHome()}/j2objc"

        String sourcepathArg = Utils.joinedPathArg(sourcepathDirs)

        List<DopplDependency> dopplLibs = getTranslateDopplLibs(_buildContext, testBuild)
        def libs = Utils.dopplJarLibs(dopplLibs)

        //Classpath arg for translation. Includes user specified jars, j2objc 'standard' jars, and doppl dependency libs
        UnionFileCollection classpathFiles = new UnionFileCollection([
                project.files(getTranslateClasspaths()),
                project.files(Utils.j2objcLibs(getJ2objcHome(), getTranslateJ2objcLibs())),
                project.files(libs)
        ])

        // TODO: comment explaining ${project.buildDir}/classes
        String classpathArg = Utils.joinedPathArg(classpathFiles) +
                              Utils.pathSeparator() + "${project.buildDir}/classes"

        // Source files arguments
        List<String> srcFilesArgs = []
        int srcFilesArgsCharCount = 0
        for (File file in srcFilesToTranslate) {
            String filePath = file.getPath()
            srcFilesArgs.add(filePath)
            srcFilesArgsCharCount += filePath.length() + 1
        }

        // Handle command line that's too long
        // Allow up to 2,000 characters for command line excluding src files
        // http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html#commandlineargfile
        if (srcFilesArgsCharCount + 2000 > Utils.maxArgs()) {
            File srcFilesArgFile = new File(getTemporaryDir(), srcFilesArgFilename);
            FileWriter writer = new FileWriter(srcFilesArgFile);
            writer.append(srcFilesArgs.join('\n'));
            writer.close()
            // Replace src file arguments by referencing file
            srcFilesArgs = ["@${srcFilesArgFile.path}".toString()]
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        List<String> mappingFiles = new ArrayList<>()

        String path = mappingsInputPath()
        if (path != null) {
            if(!project.file(path).exists())
            {
                throw new FileNotFoundException("mappingsInput '"+ path +"' does not exist")
            }

            mappingFiles.add(path);
        }

        Map<String, String> allPrefixes = new HashMap<>(prefixMap)

        for (DopplDependency lib : dopplLibs) {

            String mappingPath = Utils.findDopplLibraryMappings(lib.dependencyFolderLocation())
            if (mappingPath != null && !mappingPath.isEmpty()) {
                mappingFiles.add(mappingPath)
            }

            Properties prefixPropertiesFromFile = Utils.findDopplLibraryPrefixes(lib.dependencyFolderLocation())
            if (prefixPropertiesFromFile != null) {
                for (String name : prefixPropertiesFromFile.propertyNames()) {
                    allPrefixes.put(name, (String) properties.get(name))
                }
            }
        }

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable

                // Arguments
                args "-d", srcDir
                if(emitLineDirectives)
                {
                    args "-g", ''
                }
//                args "--strip-reflection", ''
                args "--swift-friendly", ''
//                args "-Xtranslate-classfiles", ''
                args "--package-prefixed-filenames", ''
                if (!testTranslate) {
                    args "--output-header-mapping", new File(srcDir, project.name + ".mappings").absolutePath
                }
                if (getIgnoreWeakAnnotations()) {
                    args "--ignore-weak-annotation", ''
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

                // File Inputs
                srcFilesArgs.each { String arg ->
                    // Can be list of src files or a single @/../srcFilesArgFile reference
                    args arg
                }

                setStandardOutput stdout
                setErrorOutput stderr
            })

        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
        }
    }
}
