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

import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import com.google.common.annotations.VisibleForTesting
import groovy.transform.CompileStatic
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil

/**
 * j2objcConfig is used to configure the plugin with the project's build.gradle.
 */
@CompileStatic
class J2objcConfig {
    public static final String TRANSLATE_ARC_ARG = '-use-arc'

    static J2objcConfig from(Project project) {
        return project.extensions.findByType(J2objcConfig)
    }

    final protected Project project

    J2objcConfig(Project project) {
        assert project != null
        this.project = project

        // Can't be in subdirectory as podspec paths must be relative and not traverse parent ('..')
//        destPodspecDir = new File(project.buildDir, 'j2objcOutputs').absolutePath
//        destSrcMainDir = new File(project.buildDir, 'j2objcOutputs/src/main').absolutePath
//        destSrcTestDir = new File(project.buildDir, 'j2objcOutputs/src/test').absolutePath


        // Provide defaults for assembly output locations.
        destLibDir = new File(project.buildDir, 'j2objcOutputs/lib').absolutePath
        destJavaJarDir = new File(project.buildDir, 'libs').absolutePath

        destDoppelFolder = new File(project.buildDir, 'doppel').absolutePath
        doppelDependencyExploded = new File(project.buildDir, 'doppelDependencyExploded').absolutePath
    }

    /**
     * Local exploded dir for doppel files
     */
    String destDoppelFolder = null

    String doppelDependencyExploded = null

    // Private helper methods
    // Should use instead of accessing client set 'dest' strings
    File getDestLibDirFile() {
        return project.file(destLibDir)
    }

    File getDestDoppelDirFile(){
        return project.file(destDoppelFolder)
    }

    /**
     * Exact required version of j2objc.
     */
    String j2objcVersion = '1.2'

    /**
     * Where to assemble generated main libraries.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs
     */
//    String destPodspecDir = null

    /**
     * Where to assemble generated main libraries.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/lib
     */
    String destLibDir = null

    String destJavaJarDir = null;

    String frameworkName = null

    /**
     * Where to assemble generated main source and resources files.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/src/main
     */
//    String destSrcMainDir = null

    /**
     * Where to assemble generated test source and resources files.
     * <p/>
     * Can be the same directory as destDir
     * Defaults to $buildDir/j2objcOutputs/src/test
     */
//    String destSrcTestDir = null

    boolean useArc = false;
    boolean includeJavaSource = false;

    boolean checkJ2objcVersionExplicit = false;

    String mappingsInput = null;
    String copyMainOutput = null;
    String copyTestOutput = null;
    boolean copyDependencies = false;

    boolean ignoreWeakAnnotations = false;

    /*File getDestSrcDirFile(String sourceSetName, String fileType) {
        assert sourceSetName in ['main', 'test']
        assert fileType in ['objc', 'resources']

        File destSrcDir = null
        if (sourceSetName == 'main') {
            destSrcDir = project.file(destSrcMainDir)
        } else if (sourceSetName == 'test') {
            destSrcDir = project.file(destSrcTestDir)
        } else {
            assert false, "Unsupported sourceSetName: $sourceSetName"
        }

        return project.file(new File(destSrcDir, fileType))
    }*/

    /*File getDestPodspecDirFile() {
        return project.file(destPodspecDir)
    }*/

    /**
     * Generated source files directories, e.g. from dagger annotations.
     */
    // Default location for generated source files using annotation processor compilation,
    // per sourceSets.main.output.classesDir.
    // However, we cannot actually access sourceSets.main.output.classesDir here, because
    // the Java plugin convention may not be applied at this time.
    List<String> generatedSourceDirs = ['build/classes/main', 'build/generated/source/apt/main']

    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedSourceDirs(String... generatedSourceDirs) {
        appendArgs(this.generatedSourceDirs, 'generatedSourceDirs', true, generatedSourceDirs)
    }

    List<String> generatedTestSourceDirs = ['build/classes/test', 'build/generated/source/apt/test']

    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedTestSourceDirs(String... generatedTestSourceDirs) {
        appendArgs(this.generatedTestSourceDirs, 'generatedTestSourceDirs', true, generatedTestSourceDirs)
    }

    /**
     * Command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     */
    List<String> cycleFinderArgs = new ArrayList<>()

    /**
     * Add command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     *
     * @param cycleFinderArgs add args for 'cycle_finder' tool
     */
    void cycleFinderArgs(String... cycleFinderArgs) {
        appendArgs(this.cycleFinderArgs, 'cycleFinderArgs', true, cycleFinderArgs)
    }

    /**
     * Command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     */
    List<String> translateArgs = new ArrayList<>()

    Map<String, String> translatedPathPrefix = new HashMap<>()

    /**
     * Add command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     *
     * @param translateArgs add args for the 'j2objc' tool
     */
    void translateArgs(String... translateArgs) {
        appendArgs(this.translateArgs, 'translateArgs', true, translateArgs)
    }

    List<String> processedTranslateArgs()
    {
        if(useArc && !(translateArgs.contains(TRANSLATE_ARC_ARG)))
            translateArgs(TRANSLATE_ARC_ARG);

        return translateArgs;
    }

    private void addToHeaderMap(String inpFile) {
        def properties = new Properties()

        def reader = new FileReader(project.file(inpFile))
        properties.load(reader)
        reader.close()

        properties.propertyNames().each { String key ->
            pathToTranslatedFileMap.put(key, properties.getProperty(key))
        }
    }

    void headerMappingFiles(String... f)
    {
        for (String filename : f) {
            addToHeaderMap(filename)
        }
    }

    void translatedPathPrefix(String path, String prefix)
    {
        translatedPathPrefix.put(path, prefix)
    }

    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     */
    List<String> translateClasspaths = new ArrayList<>()

    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     *
     *  @param translateClasspaths add libraries for -classpath argument
     */
    void translateClasspaths(String... translateClasspaths) {
        appendArgs(this.translateClasspaths, 'translateClasspaths', true, translateClasspaths)
    }

    /**
     * Additional Java libraries that are part of the j2objc distribution.
     * <p/>
     * For example:
     * <pre>
     * translateJ2objcLibs = ["j2objc_junit.jar", "jre_emul.jar"]
     * </pre>
     */
    // J2objc default libraries, from $J2OBJC_HOME/lib/...
    // TODO: auto add libraries based on java dependencies, warn on version differences
    List<String> translateJ2objcLibs = [
            // Comments indicate difference compared to standard libraries...
            // Memory annotations, e.g. @Weak, @AutoreleasePool
            "j2objc_annotations.jar",
            // Libraries that have CycleFinder fixes, e.g. @Weak and code removal
            "guava-19.0.jar", "j2objc_junit.jar", "jre_emul.jar",
            // Libraries that don't need CycleFinder fixes
            "javax.inject-1.jar", "jsr305-3.0.0.jar",
            "mockito-core-1.9.5.jar", "hamcrest-core-1.3.jar"/*, "protobuf_runtime.jar"*/]

    // Native build accepts empty array but throws exception on empty List<String>
    List<DoppelDependency> translateDoppelLibs = new ArrayList<>()
    List<DoppelDependency> translateDoppelTestLibs = new ArrayList<>()

    /**
     * Sets the filter on files to translate.
     * <p/>
     * If no pattern is specified, all files within the sourceSets are translated.
     * <p/>
     * This filter is applied on top of all files within the 'main' and 'test'
     * java sourceSets.  Use {@link #translatePattern(groovy.lang.Closure)} to
     * configure.
     */
    PatternSet translatePattern = null

    /**
     * Configures the {@link #translatePattern}.
     * <p/>
     * Calling this method repeatedly further modifies the existing translatePattern,
     * and will create an empty translatePattern if none exists.
     * <p/>
     * For example:
     * <pre>
     * translatePattern {
     *     exclude 'CannotTranslateFile.java'
     *     exclude '**&#47;CannotTranslateDir&#47;*.java'
     *     include '**&#47;CannotTranslateDir&#47;AnExceptionToInclude.java'
     * }
     * </pre>
     * @see
     * <a href="https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees">Gradle User Guide</a>
     */
    PatternSet translatePattern(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        if (translatePattern == null) {
            translatePattern = new PatternSet()
        }
        return ConfigureUtil.configure(cl, translatePattern)
    }

    //KPG: Review if this is still useful
    /**
     * A mapping from source file names (in the project Java sourcesets) to alternate
     * source files.
     * Both before and after names (keys and values) are evaluated using project.file(...).
     * <p/>
     * Mappings can be used to have completely different implementations in your Java
     * jar vs. your Objective-C library.  This can be especially useful when compiling
     * a third-party library and you need to provide non-trivial OCNI implementations
     * in Objective-C.
     */
    Map<String, String> translateSourceMapping = [:]

    /**
     * Adds a new source mapping.
     * @see #translateSourceMapping
     */
    void translateSourceMapping(String before, String after) {
        translateSourceMapping.put(before, after)
    }

    /**
     * The minimum iOS version to build against.  You cannot use APIs that are not supported
     * in this version.
     * <p/>
     * See https://developer.apple.com/library/ios/documentation/DeveloperTools/Conceptual/cross_development/Configuring/configuring.html#//apple_ref/doc/uid/10000163i-CH1-SW2
     */
    // Chosen to broaden compatibility for initial use
    // Maintain at one version behind current
    String minVersionIos = '8.3'

    /**
     * The minimum OS X version to build against.  You cannot use APIs that are not supported
     * in this version.
     * <p/>
     * See https://developer.apple.com/library/ios/documentation/DeveloperTools/Conceptual/cross_development/Configuring/configuring.html#//apple_ref/doc/uid/10000163i-CH1-SW2
     */
    // Oldest OS X version that supports automatic reference counting (2009 onwards)
    // Prevents Xcode error: "-fobjc-arc is not supported on versions of OS X prior to 10.6"
    String minVersionOsx = '10.6'

    /**
     * The minimum Watch OS version to build against.  You cannot use APIs that are not supported
     * in this version.
     * <p/>
     * See https://developer.apple.com/library/ios/documentation/DeveloperTools/Conceptual/cross_development/Configuring/configuring.html#//apple_ref/doc/uid/10000163i-CH1-SW2
     */
    // Chosen to broaden compatibility for initial use
    // Maintain at one version behind current
    String minVersionWatchos = '2.0'

    /**
     * Final objc source file push directory
     */
    String xcodeMainOutDir = null

    /**
     * Final objc test source file push directory
     */
    String xcodeTestOutDir = null

    Map<String, String> pathToTranslatedFileMap = new TreeMap<>()

    //KPG: Find place to call this
    protected void verifyJ2objcRequirements() {

        /*if (!Utils.isAtLeastVersion(j2objcVersion, MIN_SUPPORTED_J2OBJC_VERSION)) {
            String requestedVersion = j2objcVersion
            // j2objcVersion is used for instructing the user how to install j2objc
            // so we should use the version we need, not the bad one the user requested.
            j2objcVersion = MIN_SUPPORTED_J2OBJC_VERSION
            Utils.throwJ2objcConfigFailure(project,
                    "Must use at least J2ObjC version $MIN_SUPPORTED_J2OBJC_VERSION; you requested $requestedVersion.")
        }*/

        // Make sure we have *some* J2ObjC distribution identified.
        // This will throw a proper out-of-box error if misconfigured.
        String j2objcHome = Utils.j2objcHome(project)

        // Verify that underlying J2ObjC binary exists at all.
        File j2objcJar = Utils.j2objcJar(project)
        if (!j2objcJar.exists()) {
            Utils.throwJ2objcConfigFailure(project, "J2ObjC binary does not exist at ${j2objcJar.absolutePath}.")
        }

        //KPG: Should do a more robust check, but at this point you're either on the latest or you'll have problems
        if(checkJ2objcVersionExplicit)
        {
            checkJ2objcVersion(j2objcHome)
        }
    }

    private void checkJ2objcVersion(String j2objcHome) {
        String j2objcExecutable = "$j2objcHome/j2objc"

        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        project.logger.debug('VerifyJ2objcRequirements - projectExec:')
        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable j2objcExecutable

                // Arguments
                args "-version"

                setStandardOutput stdout
                setErrorOutput stderr
            })

        } catch (Exception exception) {  // NOSONAR
            // Likely too old to understand -version,
            // but include the error since it could be something else.
            Utils.throwJ2objcConfigFailure(project, exception.toString() + "\n\n" +
                                                    "J2ObjC binary at $j2objcHome too old, v$j2objcVersion required.")
        }
        // Yes, J2ObjC uses stderr to output the version.
        String actualVersionString = stderr.toString().trim()
        if (actualVersionString != "j2objc $j2objcVersion".toString()) {
            // Note that actualVersionString will usually already have the word 'j2objc' in it.
            Utils.throwJ2objcConfigFailure(project,
                    "Found $actualVersionString at $j2objcHome, J2ObjC v$j2objcVersion required.")
        }
    }

    // Provides a subset of "args" interface from project.exec as implemented by ExecHandleBuilder:
    // https://github.com/gradle/gradle/blob/master/subprojects/core/src/main/groovy/org/gradle/process/internal/ExecHandleBuilder.java
    // Allows the following:
    // j2objcConfig {
    //     translateArgs '--no-package-directories', '--prefixes', 'prefixes.properties'
    // }
    @VisibleForTesting
    static void appendArgs(List<String> listArgs, String nameArgs, boolean rejectSpaces, String... args) {
        verifyArgs(nameArgs, rejectSpaces, args)
        listArgs.addAll(Arrays.asList(args))
    }

    // Verify that no argument contains a space
    @VisibleForTesting
    static void verifyArgs(String nameArgs, boolean rejectSpaces, String... args) {
        if (args == null) {
            throw new InvalidUserDataException("$nameArgs == null!")
        }
        for (String arg in args) {
            if (arg.isAllWhitespace()) {
                throw new InvalidUserDataException(
                        "$nameArgs is all whitespace: '$arg'")
            }
            if (rejectSpaces) {
                if (arg.contains(' ')) {
                    String rewrittenArgs = "'" + arg.split(' ').join("', '") + "'"
                    throw new InvalidUserDataException(
                            "'$arg' argument should not contain spaces and be written out as distinct entries:\n" +
                            "$nameArgs $rewrittenArgs")
                }
            }
        }
    }

    @VisibleForTesting
    void testingOnlyPrepConfigurations() {
        // When testing we don't always want to apply the entire plugin
        // before calling finalConfigure.
        project.configurations.create('doppel')
        project.configurations.create('testDoppel')
    }
}
