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
import org.gradle.api.tasks.Input

class PodManagerTask extends DefaultTask{

    BuildContext _buildContext
    boolean testBuild

    @Input
    String getDependencyList()
    {
        StringBuilder sb = new StringBuilder()

        appendDependencyNames(_buildContext.getDependencyResolver().translateDopplLibs, sb)
        if(testBuild)
            appendDependencyNames(_buildContext.getDependencyResolver().translateDopplTestLibs, sb)

        return sb.toString()
    }

    String getJavaNames()
    {

    }

    private void appendDependencyNames(ArrayList<DopplDependency> libs, StringBuilder sb) {
        for (DopplDependency dependency : libs) {
            sb.append(dependency.dependencyFolderLocation().getName()).append("|")
        }
    }
}
