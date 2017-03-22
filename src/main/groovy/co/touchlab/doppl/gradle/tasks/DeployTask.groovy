package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.DopplDependency
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
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

    @OutputDirectory
    @Optional
    File getOutputPath() {
        String output = testCode ? DopplConfig.from(project).copyTestOutput : DopplConfig.from(project).copyMainOutput
        if (output == null)
            return null
        else
            return project.file(output)
    }

    @TaskAction
    public void runDeploy(IncrementalTaskInputs inputs)
    {
        if(getOutputPath() == null)
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
                        File outFile = findOutFile(inputFileDetails)

                        if (outFile != null) {
                            Utils.copyFileIfNewer(inputFileDetails.file, outFile)
                        }
                    }
                }
            })

            inputs.removed(new Action<InputFileDetails>() {

                @Override
                void execute(InputFileDetails inputFileDetails) {
                    File outFile = findOutFile(inputFileDetails)

                    if(outFile != null && outFile.exists())
                    {
                        outFile.delete()
                    }
                }
            })
        }
    }

    private File findOutFile(InputFileDetails inputFileDetails) {
        String inputPath = inputFileDetails.file.getPath()
        if (inputPath.startsWith(srcGenDir.getPath())) {
            String afterPath = inputPath.substring(srcGenDir.getPath().length())
            if (afterPath.startsWith("/"))
                afterPath = afterPath.substring(1)

            return new File(getOutputPath(), afterPath)
        }
        return null
    }

    private void copyEverything(FileFilter extensionFilter) {

        List<DopplDependency> dopplLibs = new ArrayList<>(getTranslateDopplLibs())

        if (getOutputPath() != null) {

            File mainOut = getOutputPath()

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
}
