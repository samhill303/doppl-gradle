package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.BuildContext
import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplDependency
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Helper to list import statements for your classes and dependencies. Copy/paste output for bridging header.
 */
@CompileStatic
class WriteBridgingHeaderTask extends DefaultTask{

    boolean testCode;

    BuildContext _buildContext;

    File srcGenDir

    List<DopplDependency> getTranslateDopplLibs() {
        DependencyResolver dependencyResolver = _buildContext.getDependencyResolver()
        return testCode ? dependencyResolver.translateDopplTestLibs : dependencyResolver.translateDopplLibs
    }

    @TaskAction
    public void writeBridgingHeader() {

        addFolderToHeader(srcGenDir, BaseChangesTask.extensionFilter, System.out)


        List<DopplDependency> dopplLibs = new ArrayList<>(getTranslateDopplLibs())

        if (testCode) {
            dopplLibs.removeAll(_buildContext.getDependencyResolver().translateDopplLibs)
        }

        for (DopplDependency lib : dopplLibs) {
            File depSource = new File(lib.dependencyFolderLocation(), "src")
            addFolderToHeader(depSource, BaseChangesTask.extensionFilter, System.out)
        }

    }

    private void addFolderToHeader(File dir, FileFilter extensionFilter, PrintStream pw) {
        File[] fromFiles = dir.listFiles(extensionFilter)
        for (File f : fromFiles) {
            if (f.isDirectory() || !f.exists() || !f.getName().endsWith(".h"))
                continue;
            pw.println("#import \"" + f.getName() + "\"")
        }
    }
}
