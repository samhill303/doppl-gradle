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

package com.github.j2objccontrib.j2objcgradle.tasks

import com.github.j2objccontrib.j2objcgradle.DoppelDependency
import com.github.j2objccontrib.j2objcgradle.J2objcConfig
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Translation task for Java to Objective-C using j2objc tool.
 */
@CompileStatic
class TranslateTask extends DefaultTask {

    // Note that translatePattern need not be @Inputs because it is solely an inputs
    // to the 2 methods below, which are already @InputFiles.

    // If the j2objc distribution changes, we want to rerun the task completely.
    // As an InputFile, if the content changes, the task will re-run in non-incremental mode.

    public static final String INCLUDE_START = "#include \""
    public static final String IMPORT_START = "#import \""

    @InputFile
    File getJ2objcJar() {
        return Utils.j2objcJar(project)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Input String getJ2objcVersion() {
        return J2objcConfig.from(project).j2objcVersion
    }

    // Source files part of the Java main sourceSet.
    @InputFiles
    FileCollection getMainSrcFiles() {
        FileTree allFiles = Utils.srcSet(project, 'main', 'java')
        if (J2objcConfig.from(project).translatePattern != null) {
            allFiles = allFiles.matching(J2objcConfig.from(project).translatePattern)
        }
        FileCollection ret = allFiles.plus(Utils.javaTrees(project, getGeneratedSourceDirs()))
//        allFiles.plus(project.fileTree(dir: "${project.buildDir}/generated/source/apt/main"))
        ret = Utils.mapSourceFiles(project, ret, getTranslateSourceMapping())
        return ret
    }

    // Source files part of the Java test sourceSet.
    @InputFiles
    FileCollection getTestSrcFiles() {
        FileTree allFiles = Utils.srcSet(project, 'test', 'java')
        if (J2objcConfig.from(project).translatePattern != null) {
            allFiles = allFiles.matching(J2objcConfig.from(project).translatePattern)
        }
        FileCollection ret = allFiles.plus(Utils.javaTrees(project, getGeneratedTestSourceDirs()))
//        allFiles.plus(project.fileTree(dir: "${project.buildDir}/generated/source/apt/test"))
        ret = Utils.mapSourceFiles(project, ret, getTranslateSourceMapping())
        return ret
    }

    // All input files that could affect translation output, except those in j2objc itself.
    @InputFiles
    FileCollection getAllInputFiles() {
        FileCollection allFiles = getMainSrcFiles()
        allFiles += getTestSrcFiles()
        allFiles += project.files(getTranslateClasspaths())
        allFiles += project.files(getTranslateSourcepaths())
        // Only care about changes in the generatedSourceDirs paths and not the contents
        // It assumes that any changes in generated code comes from change in non-generated code
        return allFiles
    }

    // Property is never used, however it is an input value as
    // the contents of the prefixes, including a prefix file, affect all translation
    // output.  We don't care about the prefix file (if any) per se, but we care about
    // the final set of prefixes.
    // NOTE: As long as all other tasks have the output of TranslateTask as its own inputs,
    // they do not also need to have the packagePrefixes as a direct input in order to
    // have correct up-to-date checks.
    @SuppressWarnings("GroovyUnusedDeclaration")
    @Input Properties getPackagePrefixes() {
        return Utils.packagePrefixes(project, translateArgs)
    }

    @Input
    String getJ2objcHome() { return Utils.j2objcHome(project) }

    @Input
    String getDoppelHome() {
        def doppelDependencyExploded = J2objcConfig.from(project).doppelDependencyExploded
        return doppelDependencyExploded
    }

    @Input
    List<String> getTranslateArgs() {
        return J2objcConfig.from(project).processedTranslateArgs()
    }

    @Input
    List<String> getTranslateClasspaths() { return J2objcConfig.from(project).translateClasspaths }

    @Input
    List<String> getTranslateSourcepaths() { return J2objcConfig.from(project).translateSourcepaths }

    @Input
    List<String> getGeneratedSourceDirs() { return J2objcConfig.from(project).generatedSourceDirs }

    @Input
    List<String> getGeneratedTestSourceDirs() { return J2objcConfig.from(project).generatedTestSourceDirs }

    @Input
    List<String> getTranslateJ2objcLibs() { return J2objcConfig.from(project).translateJ2objcLibs }

    List<DoppelDependency> getTranslateDoppelLibs() { return J2objcConfig.from(project).translateDoppelLibs }

    @Input
    Map<String, String> getTranslateSourceMapping() { return J2objcConfig.from(project).translateSourceMapping }

    @Input
    boolean getFilenameCollisionCheck() { return J2objcConfig.from(project).getFilenameCollisionCheck() }


    // Generated ObjC files
    @OutputDirectory
    File srcGenMainDir

    @OutputDirectory
    File srcGenTestDir

    @InputDirectory @Optional
    File srcMainObjcDir;

    @InputDirectory @Optional
    File srcTestObjcDir;

    @TaskAction
    void translate(IncrementalTaskInputs inputs) {
        // Exceptions must be delayed until Plugin tasks are run
        // Doing it earlier causes Gradle deadlock:
        // https://github.com/j2objc-contrib/j2objc-gradle/issues/585
        if(Utils.failGradleVersion(false))
            return;

        List<String> translateArgs = getTranslateArgs()
        // Don't evaluate this expensive property multiple times.
        FileCollection originalMainSrcFiles = getMainSrcFiles()
        FileCollection originalTestSrcFiles = getTestSrcFiles()

        logger.debug("Main source files: " + originalMainSrcFiles.getFiles().size())
        logger.debug("Test source files: " + originalTestSrcFiles.getFiles().size())

        FileCollection mainSrcFilesChanged, testSrcFilesChanged

            boolean nonSourceFileChanged = false
            mainSrcFilesChanged = project.files()
            testSrcFilesChanged = project.files()
            inputs.outOfDate(new Action<InputFileDetails>() {
                @Override
                void execute(InputFileDetails details) {
                    // We must filter by srcFiles, since all possible input files are @InputFiles to this task.
                    if (originalMainSrcFiles.contains(details.file)) {
                        getLogger().debug("New or Updated main file: " + details.file)
                        mainSrcFilesChanged += project.files(details.file)
                    } else if (originalTestSrcFiles.contains(details.file)) {
                        getLogger().debug("New or Updated test file: " + details.file)
                        testSrcFilesChanged += project.files(details.file)
                    } else {
                        nonSourceFileChanged = true
                        getLogger().debug("New or Updated non-source file: " + details.file)
                    }
                }
            })
            List<String> removedMainFileNames = new ArrayList<>()
            List<String> removedTestFileNames = new ArrayList<>()
            inputs.removed(new Action<InputFileDetails>() {
                @Override
                void execute(InputFileDetails details) {
                    // We must filter by srcFiles, since all possible input files are @InputFiles to this task.
                    if (originalMainSrcFiles.contains(details.file)) {
                        getLogger().debug("Removed main file: " + details.file.name)
                        String nameWithoutExt = details.file.name.toString().replaceFirst("\\..*", "")
                        removedMainFileNames += nameWithoutExt
                    } else if (originalTestSrcFiles.contains(details.file)) {
                        getLogger().debug("Removed test file: " + details.file.name)
                        String nameWithoutExt = details.file.name.toString().replaceFirst("\\..*", "")
                        removedTestFileNames += nameWithoutExt
                    } else {
                        nonSourceFileChanged = true
                        getLogger().debug("Removed non-source file: " + details.file)
                    }
                }
            })
            logger.debug("Removed main files: " + removedMainFileNames.size())
            logger.debug("Removed test files: " + removedTestFileNames.size())

            logger.debug("New or Updated main files: " + mainSrcFilesChanged.getFiles().size())
            logger.debug("New or Updated test files: " + testSrcFilesChanged.getFiles().size())

            FileCollection unchangedMainSrcFiles = originalMainSrcFiles - mainSrcFilesChanged
            FileCollection unchangedTestSrcFiles = originalTestSrcFiles - testSrcFilesChanged
            logger.debug("Unchanged main files: " + unchangedMainSrcFiles.getFiles().size())
            logger.debug("Unchanged test files: " + unchangedTestSrcFiles.getFiles().size())

            if (nonSourceFileChanged) {
                // A change outside of the source set directories has occurred, so an incremental build isn't possible.
                // The most common such change is in the JAR for a dependent library, for example if Java project
                // that this project depends on had its source changed and was recompiled.
                Utils.projectClearDir(project, srcGenMainDir)
                Utils.projectClearDir(project, srcGenTestDir)
                mainSrcFilesChanged = originalMainSrcFiles
                testSrcFilesChanged = originalTestSrcFiles
            } else {
                // All changes were within srcFiles (i.e. in a Java source-set).
                int translatedFiles = 0
                if (srcGenMainDir.exists()) {
                    translatedFiles += deleteRemovedFiles(removedMainFileNames, srcGenMainDir)
                }
                if (srcGenTestDir.exists()) {
                    translatedFiles += deleteRemovedFiles(removedTestFileNames, srcGenTestDir)
                }


            }


        if (getFilenameCollisionCheck()) {
            Utils.filenameCollisionCheck(getMainSrcFiles())
            Utils.filenameCollisionCheck(getTestSrcFiles())
        }

        // Translate main code.
        UnionFileCollection sourcepathDirs = new UnionFileCollection([
                project.files(Utils.srcSet(project, 'main', 'java').getSrcDirs()),
                project.files(getTranslateSourcepaths()),
                project.files(getGeneratedSourceDirs())
        ])
        StringBuilder sb = new StringBuilder()
        for (FileCollection collection: sourcepathDirs.sources ) {
            for (File file : collection.files) {
                sb.append(file.getPath()).append(' ')
            }
        }

        doTranslate(sourcepathDirs, srcMainObjcDir, srcGenMainDir, translateArgs, mainSrcFilesChanged, "mainSrcFilesArgFile", false)

        // Translate test code. Tests are never built with --build-closure; otherwise
        // we will get duplicate symbol errors.
        // There is an edge-case that will fail: if the main and test code depend on
        // some other library X AND use --build-closure to translate X AND the API of X
        // needed by the test code is not a subset of the API of X used by the main
        // code, compilation will fail. The solution is to just build the whole library
        // X as a separate j2objc project and depend on it.
        List<String> testTranslateArgs = new ArrayList<>(translateArgs)
        testTranslateArgs.removeAll('--build-closure')
        sourcepathDirs = new UnionFileCollection([
                project.files(Utils.srcSet(project, 'main', 'java').getSrcDirs()),
                project.files(Utils.srcSet(project, 'test', 'java').getSrcDirs()),
                project.files(getTranslateSourcepaths()),
                project.files(getGeneratedSourceDirs()),
                project.files(getGeneratedTestSourceDirs())
        ])
        doTranslate(sourcepathDirs, srcTestObjcDir, srcGenTestDir, testTranslateArgs, testSrcFilesChanged, "testSrcFilesArgFile", true)
    }

    int deleteRemovedFiles(List<String> removedFileNames, File dir) {
        FileCollection destFiles = project.files(project.fileTree(
                dir: dir, includes: ["**/*.h", "**/*.m"]))

        // With --build-closure, files outside the source set can end up in the srcGen
        // directory from prior translations.
        // So only remove translated .h and .m files which has no corresponding .java files anymore
        destFiles.each { File file ->
            String nameWithoutExt = file.name.toString().replaceFirst("\\..*", "")
            // TODO: Check for --no-package-directories when deciding whether
            // to compare file name vs. full path.
            if (removedFileNames.contains(nameWithoutExt)) {
                file.delete()
            }
        }
        // compute the number of translated files
        return destFiles.getFiles().size()
    }

    void doTranslate(UnionFileCollection sourcepathDirs, File nativeSourceDir, File srcDir, List<String> translateArgs,
                     FileCollection srcFilesToTranslate, String srcFilesArgFilename, boolean testTranslate) {

        if(nativeSourceDir != null && nativeSourceDir.exists()){
            Utils.projectCopy(project, {
                includeEmptyDirs = false
                from nativeSourceDir
                into srcDir
            })
        }

        int num = srcFilesToTranslate.getFiles().size()
        logger.info("Translating $num files with j2objc...")
        if (srcFilesToTranslate.getFiles().size() == 0) {
            logger.info("No files to translate; skipping j2objc execution")
            return
        }

        String j2objcExecutable = "${getJ2objcHome()}/j2objc"
        List<String> windowsOnlyArgs = new ArrayList<String>()
        if (Utils.isWindows()) {
            j2objcExecutable = 'java'
            windowsOnlyArgs.add('-jar')
            windowsOnlyArgs.add(getJ2objcJar().absolutePath)
        }

        String sourcepathArg = Utils.joinedPathArg(sourcepathDirs)


        def libs = Utils.doppelJarLibs(getTranslateDoppelLibs())

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

        logger.debug('TranslateTask - projectExec:')

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable
                windowsOnlyArgs.each { String windowsOnlyArg ->
                    args windowsOnlyArg
                }

                // Arguments
                args "-d", srcDir
                /*if(testTranslate)
                {
                    args "-g", ''
                }*/
                /*if(testTranslate)
                {
                    args "-use-arc", ''
                }*/
                args "--package-prefixed-filenames", ''
                args "-sourcepath", sourcepathArg
                args "-classpath", classpathArg
                translateArgs.each { String translateArg ->
                    args translateArg
                }

                // File Inputs
                srcFilesArgs.each { String arg ->
                    // Can be list of src files or a single @/../srcFilesArgFile reference
                    args arg
                }

                setStandardOutput stdout
                setErrorOutput stderr
            })


//            def generatedFiles = project.fileTree(dir: srcDir, includes: ['**/*.h', '**/*.m'])
//
//            remapHeaderLinks(generatedFiles.asList().toArray(new File[generatedFiles.size()]), srcDir, project)

        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
        }
    }

    static void remapHeaderLinks(File[] generatedFiles, File srcDir, Project project) {
        def basePath = srcDir.getPath()
        Map<String, String> pathToTranslatedFileMap = project.extensions.findByType(J2objcConfig).pathToTranslatedFileMap

        generatedFiles.each { File inFile ->

            try {
                String transformPath = inFile.getPath().substring(basePath.length())
                String outputPath = findTransformedFilePath(transformPath, project)

                if (transformPath.startsWith("/"))
                    transformPath = transformPath.substring(1)

                pathToTranslatedFileMap.put(transformPath, outputPath)

            } catch (Exception e) {
                e.printStackTrace()
//                logger.error("Move failed", e)
            }
        }

        generatedFiles.each { File inFile ->

            try {
                def parentPath = inFile.getParentFile().getPath().substring(basePath.length())
                if (parentPath.startsWith("/"))
                    parentPath = parentPath.substring(1)

                if(inFile.getPath().endsWith("IOSClass.h"))
                    println "Just a break"

                BufferedReader br = new BufferedReader(new FileReader(inFile))
                def tempFile = new File(inFile.getParentFile(), inFile.getName() + ".tmp")
                BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))
                String line;

                while ((line = br.readLine()) != null) {
                    if (line.startsWith(INCLUDE_START)) {

                        def matchString = line.substring(INCLUDE_START.length(), line.indexOf('"', INCLUDE_START
                                .length()));

                        String moddedPath = null;

                        if (pathToTranslatedFileMap.containsKey(matchString))
                        {
                            moddedPath = pathToTranslatedFileMap.get(matchString)
                        }
                        else if (pathToTranslatedFileMap.containsKey(parentPath + "/" + matchString))
                        {
                            println (parentPath + "/" + matchString)
                            moddedPath = pathToTranslatedFileMap.get(parentPath + "/" + matchString)
                        }


                        if (moddedPath != null) {
                            bw.append(INCLUDE_START).append(moddedPath).append('"').append("\n")
                        } else {
                            bw.append(line).append("\n")
                        }
                    } else if(line.startsWith(IMPORT_START)){
                        def matchString = line.substring(IMPORT_START.length(), line.indexOf('"', IMPORT_START
                                .length()));

                        String moddedPath = null;

                        if (pathToTranslatedFileMap.containsKey(matchString))
                        {
                            moddedPath = pathToTranslatedFileMap.get(matchString)
                        }
                        else if (pathToTranslatedFileMap.containsKey(parentPath + "/" + matchString))
                        {
                            println (parentPath + "/" + matchString)
                            moddedPath = pathToTranslatedFileMap.get(parentPath + "/" + matchString)
                        }


                        if (moddedPath != null) {
                            bw.append(IMPORT_START).append(moddedPath).append('"').append("\n")
                        } else {
                            bw.append(line).append("\n")
                        }
                    }

                    else {
                        bw.append(line).append("\n")
                    }
                }

                bw.close()
                br.close()
                inFile.delete()
                tempFile.renameTo(inFile)

                String transformPath = inFile.getPath().substring(basePath.length())
                if(transformPath.startsWith("/"))
                    transformPath = transformPath.substring(1)

                String outputPath = findTransformedFilePath(transformPath, project)
                def moveTo = new File(srcDir, outputPath)
                inFile.renameTo(moveTo)
            } catch (Exception e) {
                logger.error("Move failed", e)
            }
        }

        pathToTranslatedFileMap.keySet().each {String key ->
            println(key +"="+ pathToTranslatedFileMap.get(key))
        }
    }

    static String findTransformedFilePath(String transformPath, org.gradle.api.Project project) {
        String[] theBits = transformPath.split("/")
        if(theBits.length == 1)
            return transformPath

        StringBuilder sb = new StringBuilder()
        J2objcConfig config = project == null ? null : project.extensions.findByType(J2objcConfig)

        theBits.each { String bit ->
            if (bit.equals(theBits.last())) {
                sb.append(bit)
            } else {
                sb.append(bit.capitalize())
            }

            if (config != null && config.translatedPathPrefix.containsKey(sb.toString())) {
                sb = new StringBuilder(config.translatedPathPrefix.get(sb.toString()))
            }
        }

        def outputPath = sb.toString()
        outputPath
    }
}
