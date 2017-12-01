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
import co.touchlab.doppl.gradle.DopplInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.TaskAction

class HeaderMappingsTask extends DefaultTask{
    @TaskAction
    void writeHeaderMappings(){
        File javaFolder = DopplInfo.sourceBuildJavaFileMain(project)
        ConfigurableFileTree tree = project.fileTree(dir: javaFolder, includes: ["**/*.java"])
        Iterator<File> fileIter = tree.iterator()
        while (fileIter.hasNext()) {
            File file = fileIter.next()
            String javaPackageAndFile = Utils.relativePath(javaFolder, file)
            if(javaPackageAndFile.endsWith(".java"))
            {
                javaPackageAndFile = javaPackageAndFile.substring(0, javaPackageAndFile.length() - ".java".length())
                String[] parts = javaPackageAndFile.split(File.separator)
                StringBuilder sb = new StringBuilder()
                for (String part : parts) {
                    sb.append(part.capitalize())
                }

                println javaPackageAndFile.replace(File.separator, '.') + "=" + sb.toString() +".h"
            }
        }
    }
}
