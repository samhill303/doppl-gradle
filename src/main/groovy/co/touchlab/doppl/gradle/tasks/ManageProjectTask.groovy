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
import co.touchlab.doppl.gradle.DependencyResolver
import co.touchlab.doppl.gradle.DopplDependency
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jruby.embed.LocalContextScope
import org.jruby.embed.ScriptingContainer

/**
 * Helper to list import statements for your classes and dependencies. Copy/paste output for bridging header.
 */
@CompileStatic
class ManageProjectTask extends DefaultTask{

    @TaskAction
    public void writeBridgingHeader() {

        ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETON);
        InputStream inputStream = ManageProjectTask.class.getResourceAsStream("/createxcodeproj.rb");
        container.runScriptlet(inputStream, "createxcodeproj.rb")
        inputStream.close()
    }


}
