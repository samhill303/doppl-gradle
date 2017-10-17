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

import co.touchlab.doppl.gradle.DopplConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FrameworkTask extends DefaultTask{

    public boolean test
    public String sourceFolderName

    @TaskAction
    public void writePodspec()
    {
        File podspecFile = new File(project.buildDir, test ? "testdoppllib.podspec" : "doppllib.podspec")

        List<String> dependencyFolders = test ?
                                         ["dopplDependencyExploded", "dopplOnlyDependencyExploded", "testDopplDependencyExploded"] :
                                         ["dopplDependencyExploded", "dopplOnlyDependencyExploded"]

        def podspecTemplate = DopplConfig.from(project).framework.podspecTemplate(sourceFolderName, dependencyFolders, test ? "testdoppllib" : "doppllib")
        BufferedWriter writer = null
        try {
            writer = new BufferedWriter(new FileWriter(podspecFile))
            writer.write(podspecTemplate.toString())
        } finally {
            if(writer != null)
                writer.close()
        }
    }
}
