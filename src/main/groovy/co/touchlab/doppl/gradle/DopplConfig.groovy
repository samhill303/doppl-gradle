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

import co.touchlab.doppl.gradle.tasks.Utils
import com.google.common.annotations.VisibleForTesting
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil
/**
 * dopplConfig is used to configure the plugin with the project's build.gradle.
 */
@CompileStatic
class DopplConfig {
    public static final String TRANSLATE_ARC_ARG = '-use-arc'

    static DopplConfig from(Project project) {
        return project.extensions.findByType(DopplConfig)
    }

    final protected Project project

    DopplConfig(Project project) {
        assert project != null
        this.project = project

        // Provide defaults for assembly output locations.
        destLibDir = new File(project.buildDir, 'j2objcOutputs/lib').absolutePath
        destJavaJarDir = new File(project.buildDir, 'libs').absolutePath

        destDopplFolder = new File(project.buildDir, 'doppl').absolutePath
        dopplDependencyExploded = new File(project.buildDir, 'dopplDependencyExploded').absolutePath
    }

    /**
     * Local exploded dir for doppl files
     */
    String destDopplFolder = null

    String dopplDependencyExploded = null

    /**
     * Where to assemble generated main libraries.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/lib
     */
    String destLibDir = null

    String destJavaJarDir = null;

    boolean useArc = false;

    String mappingsInput = null;
    List<String> copyMainOutput = new ArrayList<>();
    List<String> copyTestOutput = new ArrayList<>();
    boolean copyDependencies = false;

    boolean emitLineDirectives = false;

    boolean ignoreWeakAnnotations = false;

    boolean skipDependsTasks = false

    String targetVariant = "debug"

    boolean disableAnalytics = false;

    /**
     * Additional generated source files directories
     */
    List<String> generatedSourceDirs = new ArrayList<>()
    List<String> generatedTestSourceDirs = new ArrayList<>()

    /**
     * Some transforms will need to replace existing classes. These dirs are handled special.
     */
    List<String> overlaySourceDirs = new ArrayList<>();

    /**
     * Command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     */
    List<String> cycleFinderArgs = new ArrayList<>()

    /**
     * Command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     */
    List<String> translateArgs = new ArrayList<>()

    Map<String, String> translatedPathPrefix = new HashMap<>()

    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     *
     *  TODO: We should review some of these more open ended arguments. I doubt we'd ever need
     *  to add an external jar. The only reasonable scenario would be outside objc that was already
     *  translated and added manually.
     */
    List<String> translateClasspaths = new ArrayList<>()

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

    PatternSet testIdentifier = null

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
    Map<String, String> pathToTranslatedFileMap = new TreeMap<>()

    // Private helper methods
    // Should use instead of accessing client set 'dest' strings
    File getDestLibDirFile() {
        return project.file(destLibDir)
    }

    File getDestDopplDirFile(){
        return project.file(destDopplFolder)
    }

    /**
     * Add output path for objective c files
     */
    void copyMainOutput(String... paths) {
        for (String p : paths) {
            this.copyMainOutput.add(p)
        }
    }

    /**
     * Add output path for objective c test files
     */
    void copyTestOutput(String... paths) {
        for (String p : paths) {
            this.copyTestOutput.add(p)
        }
    }

    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedSourceDirs(String... generatedSourceDirs) {
        Utils.appendArgs(this.generatedSourceDirs, 'generatedSourceDirs', true, generatedSourceDirs)
    }

    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void overlaySourceDirs(String... overlaySourceDirs) {
        this.overlaySourceDirs.addAll(overlaySourceDirs)
    }

    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedTestSourceDirs(String... generatedTestSourceDirs) {
        Utils.appendArgs(this.generatedTestSourceDirs, 'generatedTestSourceDirs', true, generatedTestSourceDirs)
    }

    /**
     * Add command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     *
     * @param cycleFinderArgs add args for 'cycle_finder' tool
     */
    void cycleFinderArgs(String... cycleFinderArgs) {
        Utils.appendArgs(this.cycleFinderArgs, 'cycleFinderArgs', true, cycleFinderArgs)
    }

    /**
     * Add command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     *
     * @param translateArgs add args for the 'j2objc' tool
     */
    void translateArgs(String... translateArgs) {
        Utils.appendArgs(this.translateArgs, 'translateArgs', true, translateArgs)
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
     *
     *  @param translateClasspaths add libraries for -classpath argument
     */
    void translateClasspaths(String... translateClasspaths) {
        Utils.appendArgs(this.translateClasspaths, 'translateClasspaths', true, translateClasspaths)
    }

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

    PatternSet testIdentifier(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        if (testIdentifier == null) {
            testIdentifier = new PatternSet()
        }
        return ConfigureUtil.configure(cl, testIdentifier)
    }

    /**
     * Adds a new source mapping.
     * @see #translateSourceMapping
     */
    void translateSourceMapping(String before, String after) {
        translateSourceMapping.put(before, after)
    }

    @VisibleForTesting
    void testingOnlyPrepConfigurations() {
        // When testing we don't always want to apply the entire plugin
        // before calling finalConfigure.
        project.configurations.create(DependencyResolver.CONFIG_DOPPL)
        project.configurations.create(DependencyResolver.CONFIG_TEST_DOPPL)
    }
}
