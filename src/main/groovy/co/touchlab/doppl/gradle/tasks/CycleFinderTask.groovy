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
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileCollection
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil

import java.util.zip.ZipFile

/**
 * CycleFinder task checks for memory cycles that can cause memory leaks
 * since iOS doesn't have garbage collection.
 */
class CycleFinderTask extends DefaultTask {

    BuildContext _buildContext

    FileCollection getSrcInputFiles() {
        UnionFileTree fileTree = new UnionFileTree("All Source")
        for (FileTree tree : _buildContext.getBuildTypeProvider().sourceSets(project)) {
            fileTree.add(tree)
        }
        return fileTree.matching(TranslateTask.javaPattern {
            include "**/*.java"
        })
    }

    List<String> getCycleFinderArgs() { return DopplConfig.from(project).cycleFinderArgs }

    List<String> getTranslateJ2objcLibs() { return DopplConfig.from(project).translateJ2objcLibs }

    File getReportFile() { project.file("${project.buildDir}/reports/${name}.out") }

    @TaskAction
    void cycleFinder() {

        File tempDir = File.createTempDir()
        tempDir.mkdirs()

        String cycleFinderExec = Utils.j2objcHome(project) + File.separator + 'cycle_finder'
        String jreWhitelist = Utils.j2objcHome(project) + File.separator + 'cycle_whitelist.txt'
        String jreSourceManifest = Utils.j2objcHome(project) + File.separator + 'jre_sources.mf'

        prepJreWhitelist(jreWhitelist, jreSourceManifest)

        FileCollection fullSrcFiles = getSrcInputFiles()

        Set<File> allJavaDirs = TranslateTask.allJavaFolders(project, _buildContext, false)

        String sourcepathArg = Utils.joinedPathArg(allJavaDirs)

        List<DopplDependency> dopplLibs = TranslateTask.getTranslateDopplLibs(_buildContext, false)

        //Classpath arg for translation. Includes user specified jars, j2objc 'standard' jars, and doppl dependency libs
        UnionFileCollection classpathFiles = new UnionFileCollection([
                project.files(Utils.j2objcLibs(Utils.j2objcHome(project), getTranslateJ2objcLibs()))
        ])

        // TODO: comment explaining ${project.buildDir}/classes
        String classpathArg = Utils.joinedPathArg(classpathFiles) +
                              File.pathSeparator + "${project.buildDir}/classes"

        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        // Capturing group is the cycle count, i.e. '\d+'
        String cyclesFoundRegex = /(\d+) CYCLES FOUND/

        try {
            logger.debug('CycleFinderTask - projectExec:')
            Utils.projectExec(project, stdout, stderr, cyclesFoundRegex, {
                executable cycleFinderExec

                // Arguments
                args "-sourcepath", sourcepathArg
                args "-classpath", classpathArg
                args "-s", jreSourceManifest
                args "-w", jreWhitelist
                getCycleFinderArgs().each { String cycleFinderArg ->
                    args cycleFinderArg
                }

                // File Inputs
                fullSrcFiles.each { File file ->
                    args file.path
                }

                setStandardOutput stdout
                setErrorOutput stderr
            })

            logger.debug("CycleFinder found 0 cycles")

        } catch (Exception exception) {  // NOSONAR
            // ExecException is converted to InvalidUserDataException in Utils.projectExec(...)

            if (!Utils.isProjectExecNonZeroExit(exception)) {
                throw exception
            }

            print(stdout.toString());

            String cyclesFoundStr = Utils.matchRegexOutputs(stdout, stderr, cyclesFoundRegex)
            if (!cyclesFoundStr?.isInteger()) {
                String message =
                        exception.toString() + '\n' +
                        'CycleFinder completed could not find expected output.\n' +
                        'Failed Regex Match cyclesFoundRegex: ' +
                        Utils.escapeSlashyString(cyclesFoundRegex) + '\n' +
                        'Found: ' + cyclesFoundStr
                throw new InvalidUserDataException(message, exception)
            }

            // Matched (XX CYCLES FOUND), so assert on cyclesFound
            int cyclesFound = cyclesFoundStr.toInteger()
            println "Cycles Found: "+ cyclesFound
        } finally {
            // Write output always.
            File reportFile = getReportFile()
            reportFile.getParentFile().mkdirs()
            reportFile.write(Utils.stdOutAndErrToLogString(stdout, stderr))
            logger.debug("CycleFinder Output: ${reportFile.path}")
        }
    }

    private void prepJreWhitelist(String jreWhitelist, String jreSourceManifest) {
        File whiteListFile = new File(jreWhitelist)
        if (!whiteListFile.exists()) {
            InputStream resourceAsStream = getClass().getResourceAsStream("/cycle_whitelist.txt")
            whiteListFile.append(resourceAsStream.getBytes())
        }

        File jreSourceManifestFile = new File(jreSourceManifest)
        if (!jreSourceManifestFile.exists()) {
            String jreSourceJar = Utils.j2objcHome(project) + File.separator + 'lib/jre_emul-src.jar'
            String jreSourcePath = Utils.j2objcHome(project) + File.separator + 'lib/jre_emul-src'

            File outputDir = new File(jreSourcePath)

            File zipFileName = new File(jreSourceJar)

            def zip = new ZipFile(zipFileName)
            zip.entries().each {
                if (!it.isDirectory()) {
                    def fOut = new File(outputDir, it.name)

                    new File(fOut.parent).mkdirs()

                    def fos = new FileOutputStream(fOut)
                    byte[] allBytes = zip.getInputStream(it).getBytes()
                    fos.write(allBytes)
                    fos.close()
                }
            }
            zip.close()

            List<String> sourceFiles = new ArrayList<>()
            fillSources(outputDir, sourceFiles)

            jreSourceManifestFile.write(sourceFiles.join("\n"))
        }
    }

    void fillSources(File dir, List<String> lines)
    {
        File[] files = dir.listFiles()
        for (File file : files) {
            if(file.isDirectory())
                fillSources(file, lines)
            else if(file.getName().endsWith(".java"))
                lines.add(file.getPath())
        }
    }


}
