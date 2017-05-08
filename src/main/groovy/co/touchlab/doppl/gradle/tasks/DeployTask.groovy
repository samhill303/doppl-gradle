package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Created by kgalligan on 3/15/17.
 */
@CompileStatic
class DeployTask extends BaseChangesTask{

    boolean testCode;

    @InputDirectory
    File srcGenDir

    List<DopplDependency> getTranslateDopplLibs() {
        DependencyResolver dependencyResolver = _buildContext.getDependencyResolver()
        return testCode ? dependencyResolver.translateDopplTestLibs : dependencyResolver.translateDopplLibs
    }

    @Input
    List<String> getDependencySimpleList()
    {
        List<String> depStrings = new ArrayList<>()

        List<DopplDependency> libs = getTranslateDopplLibs()
        for (DopplDependency dependency : libs) {
            depStrings.add(dependency.versionMame)
        }

        return depStrings
    }

    @Input
    @Optional
    List<String> getBridgingHeaderOutput()
    {
        return testCode ? DopplConfig.from(project).testBridgingHeaderOutput : DopplConfig.from(project).mainBridgingHeaderOutput
    }

    @OutputDirectories
    List<File> getOutputPaths() {
        List<String> output = testCode ? DopplConfig.from(project).copyTestOutput : DopplConfig.from(project).copyMainOutput
        List<File> outFiles = new ArrayList<>()
        for (String path : output) {
            outFiles.add(project.file(path))
        }
        return outFiles
    }

    @TaskAction
    public void runDeploy(IncrementalTaskInputs inputs)
    {
        if(getOutputPaths().isEmpty())
        {
            logger.debug("${name} task disabled for ${project.name}")
            return;
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

        if (!inputs.incremental)
        {
            copyEverything(extensionFilter)
        }
        else
        {
            inputs.outOfDate(new Action<InputFileDetails>() {
                @Override
                void execute(InputFileDetails inputFileDetails) {
                    if(extensionFilter.accept(inputFileDetails.file)) {

                        List<File> outputPaths = getOutputPaths()
                        for (File outPath : outputPaths) {
                            File outFile = findOutFile(outPath, inputFileDetails)

                            if (outFile != null) {
                                Utils.copyFileIfNewer(inputFileDetails.file, outFile)
                            }
                        }

                    }
                }
            })

            inputs.removed(new Action<InputFileDetails>() {

                @Override
                void execute(InputFileDetails inputFileDetails) {
                    List<File> outputPaths = getOutputPaths()
                    for (File outPath : outputPaths) {

                        File outFile = findOutFile(outPath, inputFileDetails)

                        if (outFile != null && outFile.exists()) {
                            outFile.delete()
                        }
                    }
                }
            })
        }
    }

    private void writeBridgingHeader(FileFilter extensionFilter)
    {
        List<String> outputPaths = getBridgingHeaderOutput()
        if(!outputPaths.isEmpty())
        {
            for (String outPath : outputPaths) {
                File file = project.file(outPath)

                PrintWriter pw = new PrintWriter(new FileWriter(file))

                File[] fromFiles = srcGenDir.listFiles(extensionFilter)
                for (File f : fromFiles) {
                    if(f.isDirectory() || !f.exists() || !f.getName().endsWith(".h"))
                        continue;
                    pw.println("#import \""+ f.getName() +"\"")
                }

                pw.close()
            }
        }
    }

    private File findOutFile(File outputPath, InputFileDetails inputFileDetails) {
        String inputPath = inputFileDetails.file.getPath()
        if (inputPath.startsWith(srcGenDir.getPath())) {
            String afterPath = inputPath.substring(srcGenDir.getPath().length())
            if (afterPath.startsWith("/"))
                afterPath = afterPath.substring(1)

            return new File(outputPath, afterPath)
        }
        return null
    }

    private void copyEverything(FileFilter extensionFilter) {

        List<DopplDependency> dopplLibs = new ArrayList<>(getTranslateDopplLibs())


        List<File> outputPaths = getOutputPaths()
        if (!outputPaths.isEmpty()) {

            for (File outPath : outputPaths) {
                File mainOut = outPath

                mainOut.deleteDir()

                Utils.copyFileRecursive(srcGenDir, mainOut, extensionFilter)

                Properties properties = new Properties();

                if (isCopyDependencies()) {

                    if(testCode)
                    {
                        dopplLibs.removeAll(_buildContext.getDependencyResolver().translateDopplLibs)
                    }

                    for (DopplDependency lib : dopplLibs) {
                        File depSource = new File(lib.dependencyFolderLocation(), "src")

                        Utils.copyFileRecursive(depSource, new File(mainOut, lib.name), extensionFilter)
                        Properties libraryPrefixes = Utils.findDopplLibraryPrefixes(lib.dependencyFolderLocation())
                        if(libraryPrefixes != null) {
                            for (String name : libraryPrefixes.propertyNames()) {
                                properties.put(name, libraryPrefixes.get(name))
                            }
                        }
                    }
                }

                Map<String, String> prefixes = getPrefixes()
                for (String name : prefixes.keySet()) {
                    properties.put(name, prefixes.get(name))
                }

                //The output dir wouldn't have been created by now if nothing is in it
                if(prefixes.size() > 0 && mainOut.exists()) {
                    //this properties file is different than what's in the doppl packaging properties
                    //Should probably rename the other one. this is ALL of them, even from dependencies.
                    def prefixFile = new File(mainOut, "prefixes.properties")
                    def writer = new FileWriter(prefixFile)

                    properties.store(writer, null);

                    writer.close()
                }
            }
        }

        writeBridgingHeader(extensionFilter)
    }
}
