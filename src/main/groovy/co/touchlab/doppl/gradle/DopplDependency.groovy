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

package co.touchlab.doppl.gradle

import org.gradle.api.Project
import org.gradle.api.file.CopySpec

/**
 * Created by kgalligan on 6/24/16.
 */
class DopplDependency {
    String name
    String versionMame
    File dir

    File dopFile = null

    DopplDependency(String name, File dir) {
        this.name = name
        this.versionMame = name
        this.dir = dir
    }

    DopplDependency(
            String group,
            String name,
            String version,
            File explodedDir,
            File dopFile)
    {
        this.name = name

        String foldername = group + "_" + name + "_" + version

        this.versionMame = foldername

        foldername = foldername.replace('-', '_')
        foldername = foldername.replace('.', '_')
        foldername = foldername.replace(' ', '_')

        dir = new File(explodedDir, foldername)

        this.dopFile = dopFile
    }

    File dependencyFolderLocation(){
        return dir
    }

    File dependencyJavaFolder(){
        return new File(dependencyFolderLocation(), "java")
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DopplDependency that = (DopplDependency) o

        if (dir != that.dir) return false
        if (name != that.name) return false

        return true
    }

    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (dir != null ? dir.hashCode() : 0)
        return result
    }

    void expandDop(Project project)
    {
        File folderLocation = dependencyFolderLocation()
        if(dopFile != null && !folderLocation.exists())
        {
            File unzipFolder = folderLocation

            //Assume this is a sources jar
            if(dopFile.getName().endsWith(".jar"))
            {
                folderLocation.mkdirs()
                unzipFolder = dependencyJavaFolder()
            }

            project.copy { CopySpec cp ->
                cp.from project.zipTree(dopFile)
                cp.into unzipFolder
            }

            markReadOnlyRecursive(folderLocation)
        }
    }

    void markReadOnlyRecursive(File dir)
    {
        File[] files = dir.listFiles()
        for (File file : files) {
            if(file.isDirectory())
            {
                markReadOnlyRecursive(file)
            }
            else if(file.getName().endsWith(".java"))
            {
                file.setReadOnly()
            }
        }
    }
}