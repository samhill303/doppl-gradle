

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

import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree

/**
 * Created by kgalligan on 3/11/17.
 */
class AndroidBuildTypeProvider implements BuildTypeProvider{
    def variants = null
    private DopplConfig dopplConfig

    AndroidBuildTypeProvider(Project project) {
        dopplConfig = DopplConfig.from(project)
        if (project.plugins.findPlugin("com.android.application") || project.plugins.findPlugin("android") ||
            project.plugins.findPlugin("com.android.test")) {
            variants = "applicationVariants";
        } else if (project.plugins.findPlugin("com.android.library") || project.plugins.findPlugin("android-library")) {
            variants = "libraryVariants";
        } else {
            throw new ProjectConfigurationException("The android or android-library plugin must be applied to the project", null)
        }
    }

    @Override
    void configureDependsOn(Project project, Task upstreamTask, Task downstreamTask) {
        if(!dopplConfig.skipDependsTasks) {
            project.android[variants].all { variant ->
                def javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
                //TODO: Figure out how to do this, but it's not a HUGE deal
//                javaCompile.dependsOn upstreamTask
                downstreamTask.dependsOn javaCompile
            }
        }
    }

    @Override
    void configureTestDependsOn(Project project, Task upstreamTask, Task downstreamTask) {
        if(!dopplConfig.skipDependsTasks) {
            Task testTask = project.tasks.getByName("test")
            testTask.dependsOn upstreamTask
            downstreamTask.dependsOn testTask
        }
    }

    @Override
    List<FileTree> testSourceSets(Project project) {
        return sourceSets(project, true)
    }

    @Override
    List<FileTree> sourceSets(Project project) {
        return sourceSets(project, false)
    }

    List<FileTree> sourceSets(Project project, boolean test) {

        List<FileTree> paths = new ArrayList<>()

        boolean variantFound = false

        project.android[variants].all { variant ->
            if(variant.getName() == dopplConfig.targetVariant) {
                variantFound = true
                List<FileTree> javaSources = test ? variant.unitTestVariant.variantData.javaSources : variant.variantData.javaSources
                for (FileTree s : javaSources) {
                    String path = Utils.dirFromFileTree(s).getPath()
                    if(!path.contains("/rs/") && !path.contains("/aidl/"))
                        paths.add(s)
                }
                List<File> generatedSourceFolders = test ? variant.unitTestVariant.variantData.extraGeneratedSourceFolders : variant.variantData.extraGeneratedSourceFolders
                if(generatedSourceFolders != null)
                {
                    for (File file : generatedSourceFolders) {
                        paths.add(project.fileTree(dir: file, includes: ["**/*.java"]))
                    }
                }
            }
        }

        if(!variantFound)
        {
            throw new ProjectConfigurationException("Variant ${dopplConfig.targetVariant} not found for project. Please set dopplConfig.targetVariant.", null)
        }

        ArrayList<String> generatedSourceDirs = test ? dopplConfig.generatedTestSourceDirs : dopplConfig.generatedSourceDirs

        if(generatedSourceDirs.size() > 0)
        {
            List<ConfigurableFileTree> trees = Utils.javaTrees(project, generatedSourceDirs)
            for (ConfigurableFileTree tree : trees) {
                paths.add(tree)
            }
        }

        //annotationProcessor doesn't show up in collection of added source directories (or I haven't found it)
        if(test) {
            Configuration testAnnotationProcessorConfig = project.configurations.findByName("testAnnotationProcessor")
            if (testAnnotationProcessorConfig != null) {
                paths.add(project.fileTree('build/generated/source/apt/test/' + dopplConfig.targetVariant))
            }
        }else{
            Configuration annotationProcessorConfig = project.configurations.findByName("annotationProcessor")
            if(annotationProcessorConfig != null)
            {
                paths.add(project.fileTree('build/generated/source/apt/'+ dopplConfig.targetVariant))
            }
        }

        return paths
    }
}
