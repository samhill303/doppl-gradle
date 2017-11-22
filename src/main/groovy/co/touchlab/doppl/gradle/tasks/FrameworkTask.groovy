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
import co.touchlab.doppl.gradle.DopplPlugin
import co.touchlab.doppl.gradle.FrameworkConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FrameworkTask extends DefaultTask {

    public boolean test
    public BuildContext _buildContext

    @TaskAction
    public void writePodspec() {
        String specName = test ? "testdoppllib" : "doppllib"
        File podspecFile = new File(project.buildDir, "${specName}.podspec")

        List<String> dependencyFolders = test ?
                                         [
                                                 DopplPlugin.FOLDER_J2OBJC_OUT_MAIN,
                                                 DopplPlugin.FOLDER_J2OBJC_OUT_TEST,
                                                 DopplPlugin.FOLDER_DOPPL_DEP_EXPLODED,
                                                 DopplPlugin.FOLDER_DOPPL_ONLY_DEP_EXPLODED,
                                                 DopplPlugin.FOLDER_TEST_DOPPL_DEP_EXPLODED
                                         ] :
                                         [
                                                 DopplPlugin.FOLDER_J2OBJC_OUT_MAIN,
                                                 DopplPlugin.FOLDER_DOPPL_DEP_EXPLODED,
                                                 DopplPlugin.FOLDER_DOPPL_ONLY_DEP_EXPLODED
                                         ]

        List<String> sourceFolders = new ArrayList<>()
        DopplConfig dopplConfig = DopplConfig.from(project)
        if(dopplConfig.emitLineDirectives) {
            sourceFolders.add(DopplPlugin.DOPPL_JAVA_MAIN)
            if(test)
            {
                sourceFolders.add(DopplPlugin.DOPPL_JAVA_TEST)
            }

            List<DopplDependency> dopplLibs = TranslateTask.getTranslateDopplLibs(_buildContext, test)

            for (DopplDependency dep : dopplLibs) {
                File folder = dep.dependencyJavaFolder()
                if (folder.exists() && folder.isDirectory()) {
                    String relativePath = Utils.relativePath(project.buildDir, folder)
                    sourceFolders.add(relativePath)
                }
            }
        }

        FrameworkConfig config = test ? FrameworkConfig.findTest(project) : FrameworkConfig.findMain(project)

        def podspecTemplate = config.podspecTemplate(project, sourceFolders, dependencyFolders, specName, _buildContext)
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
