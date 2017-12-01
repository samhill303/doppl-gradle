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
import co.touchlab.doppl.gradle.DopplInfo
import co.touchlab.doppl.gradle.DopplPlugin
import co.touchlab.doppl.gradle.FrameworkConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class FrameworkTask extends DefaultTask {

    public boolean test
    public BuildContext _buildContext
    public List<File> srcDirs = new ArrayList<>()

    public void addSrdDir(File f)
    {
        srcDirs.add(f)
    }

    static File podspecFile(Project project, boolean test)
    {
        String specName = podspecName(test)
        File podspecFile = new File(project.buildDir, "${specName}.podspec")
        return podspecFile
    }

    static String podspecName(boolean test) {
        test ? "testdoppllib" : "doppllib"
    }

    @TaskAction
    public void writePodspec() {
        if(test && DopplConfig.from(project).skipTests)
            return

        String specName = podspecName(test)
        File podspecFile = podspecFile(project, test)

        List<File> depFolders = new ArrayList<>()
        depFolders.add(DopplInfo.dependencyBuildJarFileForPhase(project, "main"))
        depFolders.add(DopplInfo.dependencyExplodedDopplFile(project))
        depFolders.add(DopplInfo.dependencyExplodedDopplOnlyFile(project))

        List<File> srcFolders = new ArrayList<>()
        srcFolders.add(DopplInfo.sourceBuildJavaFileMain(project))
        for (File file : srcDirs) {
            srcFolders.add(file)
        }

        if(test)
        {
            depFolders.add(DopplInfo.dependencyBuildJarFileForPhase(project, "test"))
            depFolders.add(DopplInfo.dependencyExplodedTestDopplFile(project))
            srcFolders.add(DopplInfo.sourceBuildJavaFileTest(project))
        }

        FrameworkConfig config = test ? FrameworkConfig.findTest(project) : FrameworkConfig.findMain(project)

        def podspecTemplate = config.podspecTemplate(project, srcFolders, depFolders, specName, _buildContext)
        BufferedWriter writer = null
        try {
            writer = new BufferedWriter(new FileWriter(podspecFile))
            writer.write(podspecTemplate.toString())
        } finally {
            if (writer != null)
                writer.close()
        }
    }
}
