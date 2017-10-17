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
import groovy.swing.SwingBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DopplSetupWizardTask  extends DefaultTask{
    @TaskAction
    public void runMe(){
        Console console = System.console()
//        j2objc.home=/Users/kgalligan/devel/j2objc/dist
        DopplConfig from = DopplConfig.from(project)
        String homeOrNull = Utils.j2objcHomeOrNull(project)

        if(project.hasProperty("dopplPath"))
        {
            Utils.writeLocalProperty(project, "j2objc.home", project.getProperties().get("dopplPath").toString())
        }

        if(project.hasProperty("createIos"))
        {
            String iosPath = project.getProperties().get("createIos").toString()
            project.logger.lifecycle("Create ios project at: "+ iosPath)
        }

        if(project.hasProperty("createIosTest"))
        {

        }

        /*while (homeOrNull == null)
        {

            *//*String line = console.readLine("J2objc home not defined. Please enter path: ")
            if(line == null || line.trim().length() == 0)
                continue

            line = line.trim()
            File file = new File(line)
            if(file.exists() && file.isDirectory())
            {
                Utils.writeLocalProperty(project, "j2objc.home", line)
            }

            homeOrNull = Utils.j2objcHomeOrNull(project)*//*
        }*/
    }

    void writePodfile(String module, String iosProjectName)
    {
        def podfileTemplate = """platform :ios, '9.0'

target 'ios' do
  # Comment the next line if you're not using Swift and don't want to use dynamic frameworks
  use_frameworks!

  # Pods for ios
  pod 'doppllib', :path => '../${module}/build'

end  
  """
    }
}
