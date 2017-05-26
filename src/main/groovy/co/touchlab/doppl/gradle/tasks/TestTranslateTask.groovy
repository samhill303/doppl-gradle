package co.touchlab.doppl.gradle.tasks

import co.touchlab.doppl.gradle.DopplConfig
import co.touchlab.doppl.gradle.analytics.DopplAnalytics
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet


class TestTranslateTask extends BaseChangesTask {
    List<FileTree> sourceSets

    @OutputFile
    File output

    @InputFiles
    FileCollection getSrcFiles() {
        sourceSets = _buildContext.getBuildTypeProvider().testSourceSets(project)
        return replaceOverlayFilterJava(sourceSets)
    }

    private FileCollection replaceOverlayFilterJava(List<FileTree> sourceDirs) {

        FileTree allFiles = new UnionFileTree("testClasses", (Collection<? extends FileTree>) sourceDirs)

        DopplConfig dopplConfig = DopplConfig.from(project)

        FileCollection resultCollection = allFiles.matching(new PatternSet().include("**/*Test.java"))

        if (dopplConfig.testTranslatePattern != null) {
            resultCollection = resultCollection.matching(dopplConfig.testTranslatePattern)
        }

        return Utils.mapSourceFiles(project, resultCollection, dopplConfig.getTranslateSourceMapping())
    }

    @TaskAction
    void writeTestList() {
        DopplConfig dopplConfig = DopplConfig.from(project)

        if (!dopplConfig.disableAnalytics) {
            new DopplAnalytics(dopplConfig, Utils.findVersionString(project, Utils.j2objcHome(project))).execute()
        }

        // Don't evaluate this expensive property multiple times.
        FileCollection originalSrcFiles = getSrcFiles()

        List<String> classes = new ArrayList<>()
        String filepath

        for (File file : originalSrcFiles) {
            for (FileTree ft : sourceSets) {
                def temp = processFileTree(file, ft)
                if (temp != null)
                    filepath = temp
            }

            if (filepath != null)
                classes.add(filepath)
        }

        output.write(String.join(",\n", classes))
    }

    private String processFileTree(File file, FileTree tree) {
        String filepath
        String treeDir = tree.dir.toString()
        int index = file.path.indexOf(treeDir)

        if (index >= 0) {
            filepath = file.path.replace("$treeDir/", "")
            filepath = filepath.replace('/', '.')
            filepath = filepath.replace(".java", ".class")
        }

        return filepath
    }
}
