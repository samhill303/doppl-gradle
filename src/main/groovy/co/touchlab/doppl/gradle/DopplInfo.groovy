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

class DopplInfo {
    /**
     * Path where we write jar files for dependencies that will be
     * transformed into Objective-C.
     *
     * @param project
     * @return
     */

    public static final String MAIN = "main"
    public static final String TEST = "test"
    public static final String DOPPL_BUILD = "dopplBuild"
    public static final String DEPENDENCIES = "dependencies"
    public static final String JAR = "jar"
    public static final String SOURCE = "source"
    public static final String JAVA = "java"

    private static File rootBuildFile(Project project) {
        new File(project.buildDir, DOPPL_BUILD)
    }

    /**
     * Root folder for exploded dop archives
     * @param project
     * @return
     */
    static File dependencyExplodedFile(Project project)
    {
        return new File(project.buildDir, DopplPlugin.FOLDER_DOPPL_DEP)
    }

    static File dependencyExplodedDopplFile(Project project)
    {
        return new File(dependencyExplodedFile(project), DopplPlugin.FOLDER_DOPPL_DEP_EXPLODED)
    }

    static File dependencyExplodedDopplOnlyFile(Project project)
    {
        return new File(dependencyExplodedFile(project), DopplPlugin.FOLDER_DOPPL_ONLY_DEP_EXPLODED)
    }

    static File dependencyExplodedTestDopplFile(Project project)
    {
        return new File(dependencyExplodedFile(project), DopplPlugin.FOLDER_TEST_DOPPL_DEP_EXPLODED)
    }

    static File dependencyBuildFile(Project project)
    {
        return new File(rootBuildFile(project), DEPENDENCIES)
    }

    static File dependencyBuildJarFile(Project project)
    {
        return new File(dependencyBuildFile(project), JAR)
    }

    static File dependencyBuildJarFileForPhase(Project project, String phase)
    {
        return new File(dependencyBuildJarFile(project), phase)
    }

    static File sourceBuildFile(Project project)
    {
        return new File(rootBuildFile(project), SOURCE)
    }

    static File sourceBuildJavaFile(Project project)
    {
        return new File(sourceBuildFile(project), JAVA)
    }

    static File sourceBuildJavaFileForPhase(Project project, String phase)
    {
        return new File(sourceBuildJavaFile(project), phase)
    }

    static File sourceBuildJavaFileMain(Project project)
    {
        return sourceBuildJavaFileForPhase(project, MAIN)
    }

    static File sourceBuildJavaFileTest(Project project)
    {
        return sourceBuildJavaFileForPhase(project, TEST)
    }

    static File sourceBuildJarFile(Project project)
    {
        return new File(sourceBuildFile(project), JAR)
    }

    static File sourceBuildJarFileForPhase(Project project, String phase)
    {
        return new File(sourceBuildJarFile(project), phase)
    }

    static File sourceBuildJarFileMain(Project project)
    {
        return sourceBuildJarFileForPhase(project, MAIN)
    }

    static File sourceBuildJarFileTest(Project project)
    {
        return sourceBuildJarFileForPhase(project, TEST)
    }

    /*static File dependencyBuildJarPathMain(Project project)
    {
        File build = new File(dependencyBuildJarPath(project), MAIN)
        return build
    }
    static File dependencyBuildJarPathTest(Project project)
    {
        File build = new File(dependencyBuildJarPath(project), TEST)
        return build
    }*/


}
