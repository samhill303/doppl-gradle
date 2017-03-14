/*

 */

package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.BuildTypeProvider
import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

/**
 * Translation task for Java to Objective-C using j2objc tool.
 */
@CompileStatic
class TranslateTask extends DefaultTask {

    BuildTypeProvider _buildTypeProvider;

    // Source files part of the Java main sourceSet.
    @InputFiles
    FileCollection getMainSrcFiles() {
        List<FileTree> sourceSets = _buildTypeProvider.sourceSets(project)
        return replaceOverlayFilterJava(sourceSets)
    }

    Set<File> getMainSrcDirs() {
        Set<File> allFiles = new HashSet<>()
        for (FileTree genPath : _buildTypeProvider.sourceSets(project)) {
            allFiles.add(Utils.dirFromFileTree(genPath))
        }
        return allFiles
    }

    Set<File> getTestSrcDirs() {
        Set<File> allFiles = new HashSet<>()
        allFiles.addAll(getMainSrcDirs())

        for (FileTree genPath : _buildTypeProvider.testSourceSets(project)) {
            allFiles.add(Utils.dirFromFileTree(genPath))
        }

        return allFiles
    }

    // Source files part of the Java test sourceSet.
    @InputFiles
    FileCollection getTestSrcFiles() {
        List<FileTree> sourceSets = _buildTypeProvider.testSourceSets(project)
        return replaceOverlayFilterJava(sourceSets)
    }

    @Input
    Map<String, String> getPrefixes() {
        DopplConfig.from(project).translatedPathPrefix
    }

    @Input
    String getJ2objcHome() { return Utils.j2objcHome(project) }

    @Input
    List<String> getTranslateArgs() {
        return DopplConfig.from(project).processedTranslateArgs()
    }

    @Input
    List<String> getTranslateClasspaths() { return DopplConfig.from(project).translateClasspaths }

    @Input
    List<String> getTranslateJ2objcLibs() { return DopplConfig.from(project).translateJ2objcLibs }

    @Input
    boolean getIgnoreWeakAnnotations() { return DopplConfig.from(project).ignoreWeakAnnotations }

    @Input
    boolean getDeleteStaleCopyFiles() { return DopplConfig.from(project).deleteStaleCopyFiles }

    List<DopplDependency> getTranslateDopplLibs() { return DopplConfig.from(project).translateDopplLibs }

    List<DopplDependency> getTranslateDopplTestLibs() { return DopplConfig.from(project).translateDopplTestLibs }

    @Input
    Map<String, String> getTranslateSourceMapping() { return DopplConfig.from(project).translateSourceMapping }

    // Generated ObjC files
    @OutputDirectory
    File srcGenMainDir

    @OutputDirectory
    File srcGenTestDir

    @InputDirectory @Optional
    File srcMainObjcDir;

    @InputDirectory @Optional
    File srcTestObjcDir;

    @Input String mappingsInputPath() {
        DopplConfig.from(project).mappingsInput
    }

    @OutputDirectory File copyMainOutputPath() {

        String output = DopplConfig.from(project).copyMainOutput
        if (output == null)
            return null
        else
            return project.file(output)
    }

    @Input boolean copyDependencies() {
        DopplConfig.from(project).copyDependencies
    }

    @OutputDirectory File copyTestOutputPath() {
        String output = DopplConfig.from(project).copyTestOutput
        if (output == null)
            return null
        else
            return project.file(output)
    }

    private FileCollection replaceOverlayFilterJava(List<FileTree> sourceDirs) {

        FileTree allFiles = new UnionFileTree("asdf", (Collection<? extends FileTree>)sourceDirs)

        DopplConfig dopplConfig = DopplConfig.from(project)

        allFiles = allFiles.matching(new PatternSet().include("**/*.java"))

        List<String> overlaySourceDirs = dopplConfig.overlaySourceDirs
        Set<String> overlayClasses = new HashSet<>()
        List<FileTree> overlayTrees = new ArrayList<>()

        for (String sd  : overlaySourceDirs) {
            ConfigurableFileTree fileTree = project.fileTree(dir: sd, includes: ["**/*.java"])
            FileTree matchingFileTree = fileTree.matching(dopplConfig.translatePattern)

            for (File overlayFile : matchingFileTree.files) {
                overlayClasses.add(overlayFile.getPath().substring(fileTree.getDir().getPath().length()))
            }

            overlayTrees.add(matchingFileTree)
        }

        Set<File> allFilesFiles = allFiles.files
        Set<File> toRemove = new HashSet<>()

        //Yeah, this is not great. TODO: better matching algo
        for (File af : allFilesFiles) {

            String sourceFilePath = af.getPath()
            for (String overlayClassFilenameSuffix : overlayClasses) {
                if(sourceFilePath.endsWith(overlayClassFilenameSuffix))
                {
                    toRemove.add(af)
                }
            }
        }

        for (FileTree tree : overlayTrees) {
            allFiles.add(tree)
        }

        FileCollection resultCollection = allFiles;

        if (dopplConfig.translatePattern != null) {
            resultCollection = resultCollection.matching(dopplConfig.translatePattern)
        }

        resultCollection = resultCollection.filter {f ->
            !toRemove.contains(f)
        }

        return Utils.mapSourceFiles(project, resultCollection, getTranslateSourceMapping())
    }

    @TaskAction
    void translate(IncrementalTaskInputs inputs) {

        DopplConfig dopplConfig = DopplConfig.from(project)
        new DependencyResolver(project, dopplConfig).configureAll()
//        DependencyResolver.configureSourceSets(project)

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

        List<File> translateSourceDirs = new ArrayList<>(getMainSrcDirs());

        logger.info("FileList-Before: "+ debugFileList(mainSrcFilesChanged))

        logger.info("FileList-After: "+ debugFileList(mainSrcFilesChanged))

        def prefixMap = getPrefixes()

        doTranslate(
                project.files(translateSourceDirs.toArray()),
                srcMainObjcDir,
                srcGenMainDir,
                translateArgs,
                prefixMap,
                mainSrcFilesChanged,
                "mainSrcFilesArgFile",
                false
        )

        Utils.projectCopy(project, {

            from originalMainSrcFiles
            into srcGenMainDir
            setIncludeEmptyDirs(false)

            if (dopplConfig.includeJavaSource) {
                include '**/*.java'
            }
            include '**/*.mappings'
        })

        if (prefixMap.size() > 0) {
            def prefixes = new File(srcGenMainDir, "prefixes.properties")
            def writer = new FileWriter(prefixes)

            for (String prefix : prefixMap.keySet()) {
                writer.append(prefix).append("=").append(prefixMap.get(prefix))
            }

            writer.close()
        }

        FileFilter extensionFilter = new FileFilter() {
            @Override
            boolean accept(File pathname) {
                String name = pathname.getName()
                return pathname.isDirectory() ||
                       name.endsWith(".h") ||
                       name.endsWith(".m") ||
                       name.endsWith(".cpp") ||
                       name.endsWith(".hpp") ||
                       name.endsWith(".java") ||
                       name.endsWith(".modulemap")
            }
        }

        List<DopplDependency> dopplLibs = new ArrayList<>(getTranslateDopplLibs())

        if (copyMainOutputPath() != null) {

            File mainOut = copyMainOutputPath()

            Utils.copyIfNewerRecursive(srcGenMainDir, mainOut, extensionFilter, getDeleteStaleCopyFiles())

            if (copyDependencies()) {

                for (DopplDependency lib : dopplLibs) {
                    File depSource = new File(lib.dependencyFolderLocation(), "src")

                    Utils.copyIfNewerRecursive(depSource, new File(mainOut, lib.name), extensionFilter, getDeleteStaleCopyFiles())
                }
            }

            //TODO: Figure out
            /*if(frameworkName() != null)
            {
                writeModulemap(mainOut, frameworkName())
            }*/
        }

        // Translate test code. Tests are never built with --build-closure; otherwise
        // we will get duplicate symbol errors.
        // There is an edge-case that will fail: if the main and test code depend on
        // some other library X AND use --build-closure to translate X AND the API of X
        // needed by the test code is not a subset of the API of X used by the main
        // code, compilation will fail. The solution is to just build the whole library
        // X as a separate j2objc project and depend on it.
        //KPG: We don't pass '--build-closure' to j2objc. Evaluate if we want to add support.
        List<String> testTranslateArgs = new ArrayList<>(translateArgs)
        testTranslateArgs.removeAll('--build-closure')

        doTranslate(
                project.files(getTestSrcDirs().toArray()),
                srcTestObjcDir,
                srcGenTestDir,
                testTranslateArgs,
                prefixMap,
                testSrcFilesChanged,
                "testSrcFilesArgFile",
                true
        )

        Utils.projectCopy(project, {
            from Utils.srcDirs(project, 'test', 'java')
            into srcGenTestDir
            if (dopplConfig.includeJavaSource) {
                include '**/*.java'
            }
            include '**/*.mappings'
        })

        if (copyTestOutputPath() != null) {
            File testOut = copyTestOutputPath()
            Utils.copyIfNewerRecursive(srcGenTestDir, testOut, extensionFilter, getDeleteStaleCopyFiles())
            if (copyDependencies()) {
                List<DopplDependency> testDopplLibs = new ArrayList<>(getTranslateDopplTestLibs())

                testDopplLibs.removeAll(dopplLibs)

                for (DopplDependency lib : testDopplLibs) {
                    File depSource = new File(lib.dependencyFolderLocation(), "src")

                    Utils.copyIfNewerRecursive(depSource, new File(testOut, lib.name), extensionFilter, getDeleteStaleCopyFiles())
                }
            }
        }
    }

    String debugFileList(FileCollection files)
    {
        Collection<File> asdf = files.files
        StringBuilder sb = new StringBuilder()
        for (File f : asdf) {
            if(sb.length())
                sb.append(", ")
            sb.append(f.getPath())
        }

        return sb.toString()
    }

    String debugStringList(Collection<String> files)
    {
        StringBuilder sb = new StringBuilder()
        for (String f : files) {
            if(sb.length())
                sb.append(", ")
            sb.append(f)
        }

        return sb.toString()
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

    void doTranslate(
            FileCollection sourcepathDirs,
            File nativeSourceDir,
            File srcDir,
            List<String> translateArgs,
            Map<String, String> prefixMap,
            FileCollection srcFilesToTranslate,
            String srcFilesArgFilename,
            boolean testTranslate) {

        if (nativeSourceDir != null && nativeSourceDir.exists()) {
            Utils.projectCopy(project, {
                includeEmptyDirs = false
                from nativeSourceDir
                into srcDir
            })
        }


        Set<File> files = srcFilesToTranslate.getFiles()
        int num = files.size()
        logger.info("Translating $num files with j2objc...")
        if (files.size() == 0) {
            logger.info("No files to translate; skipping j2objc execution")
            return
        }

        for (File f : files) {
            logger.info("Translating ${f.getPath()}")
        }

        String j2objcExecutable = "${getJ2objcHome()}/j2objc"

        String sourcepathArg = Utils.joinedPathArg(sourcepathDirs)

        List<DopplDependency> dopplLibs = new ArrayList<>(getTranslateDopplLibs())
        if (testTranslate) {
            dopplLibs.addAll(getTranslateDopplTestLibs())
        }
        def libs = Utils.dopplJarLibs(dopplLibs)

        UnionFileCollection classpathFiles = new UnionFileCollection([
                project.files(getTranslateClasspaths()),
                project.files(Utils.j2objcLibs(getJ2objcHome(), getTranslateJ2objcLibs())),
                project.files(libs)
        ])

        // TODO: comment explaining ${project.buildDir}/classes
        String classpathArg = Utils.joinedPathArg(classpathFiles) +
                              Utils.pathSeparator() + "${project.buildDir}/classes"

        logger.info("srcFilesArgs-a: "+ debugFileList(srcFilesToTranslate))

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


        List<String> mappingFiles = new ArrayList<>()

        /*File file = standardMappingFile()
        if(file.exists())
        {
            mappingFiles.add(file.getPath())
        }*/


        String path = mappingsInputPath()
        if (path != null) {
            mappingFiles.add(path);
        }

        Map<String, String> allPrefixes = new HashMap<>(prefixMap)

        for (DopplDependency lib : dopplLibs) {

            String mappingPath = Utils.findDopplLibraryMappings(lib.dependencyFolderLocation())
            if (mappingPath != null && !mappingPath.isEmpty()) {
                mappingFiles.add(mappingPath)
            }

            def prefixFile = Utils.findDopplLibraryPrefixes(lib.dependencyFolderLocation())
            if (prefixFile != null) {

                def properties = new Properties()

                def fileReader = new FileReader(prefixFile)
                properties.load(fileReader)
                fileReader.close()

                for (String name : properties.propertyNames()) {
                    allPrefixes.put(name, (String) properties.get(name))
                }
            }
        }

        logger.info("srcFilesArgs-b: "+ debugStringList(srcFilesArgs))

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable

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
//                args "--strip-reflection", ''
                args "-Xuse-javac", ''
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
