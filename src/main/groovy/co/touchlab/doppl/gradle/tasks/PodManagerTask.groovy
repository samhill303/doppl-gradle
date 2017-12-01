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
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class PodManagerTask extends DefaultTask{

    BuildContext _buildContext
    boolean testBuild
    String podfilePath
    static int taskCount = 0

    static void addPodManagerTask(TaskContainer tasks, String path, BuildContext buildContext, boolean testBuild, Task downstream, Task upstream)
    {
        Task task = tasks.create(name: "podManagerTask_${taskCount++}", type: PodManagerTask){
            _buildContext = buildContext
            testBuild = true
            podfilePath = path
        }
        downstream.dependsOn(task)
        task.dependsOn(upstream)
    }

    @Input
    String getDependencyList()
    {
        StringBuilder sb = new StringBuilder()

        appendDependencyNames(_buildContext.getDependencyResolver().translateDopplLibs, sb)
        if(testBuild)
            appendDependencyNames(_buildContext.getDependencyResolver().translateDopplTestLibs, sb)

        return sb.toString()
    }

    @Input
    String getJavaFileList()
    {
        File javaDir
        if(testBuild)
            javaDir = DopplInfo.sourceBuildJavaFileTest(project)
        else
            javaDir = DopplInfo.sourceBuildJavaFileMain(project)

        return pathFileCollection(javaDir)
    }

    private String pathFileCollection(File javaDir) {
        List<String> fileList = new ArrayList<>()
        pathCollection(fileList, javaDir, javaDir)
        Collections.sort(fileList)

        return fileList.join(File.pathSeparator)
    }

    void pathCollection(List<String> paths, File rootDir, File thisDir)
    {
        File[] files = thisDir.listFiles()
        for (File f : files) {
            if(f.isDirectory())
            {
                pathCollection(paths, rootDir, f)
            }
            else
            {
                paths.add(Utils.relativePath(rootDir, f))
            }
        }
    }

    @TaskAction
    void rebuildPod(IncrementalTaskInputs inputs) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream()
        ByteArrayOutputStream stderr = new ByteArrayOutputStream()

        try {
            Utils.projectExec(project, stdout, stderr, null, {
                executable "pod"

                args "install"

                setStandardOutput stdout
                setErrorOutput stderr

                setWorkingDir podfilePath
            })

            File podsDir = new File(project.projectDir, podfilePath)// + "Pods")
            String podspecName = FrameworkTask.podspecName(testBuild)
            File umbrellaFile = new File(podsDir, "Pods/Target Support Files/${podspecName}/${podspecName}-umbrella.h")

            if(umbrellaFile.exists())
            {
                BufferedReader reader = new BufferedReader(new FileReader(umbrellaFile))
                File outFile = new File(umbrellaFile.getParentFile(),
                        umbrellaFile.getName() + ".new")
                BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))
                String line = null
                while((line = reader.readLine()) != null)
                {
                    if(line.startsWith("#import \""))
                    {
                        writer.append(line.replace("#import", "#include"))
                    }
                    else
                    {
                        writer.append(line)
                    }
                    writer.append("\n")
                }
                reader.close()
                writer.close()
                umbrellaFile.delete()
                outFile.renameTo(umbrellaFile)
            }
            else {
                logger.warn("Umbrella header file not found. Check config.")
            }
        } catch (Exception exception) {  // NOSONAR
            // TODO: match on common failures and provide useful help
            throw exception
        }
    }

    private void appendDependencyNames(ArrayList<DopplDependency> libs, StringBuilder sb) {
        for (DopplDependency dependency : libs) {
            sb.append(dependency.dependencyFolderLocation().getName()).append("|")
        }
    }
}
