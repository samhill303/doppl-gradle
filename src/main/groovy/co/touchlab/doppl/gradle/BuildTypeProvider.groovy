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
import org.gradle.api.Task
import org.gradle.api.file.FileTree

/**
 * Created by kgalligan on 3/11/17.
 */
interface BuildTypeProvider {
    void configureDependsOn(Project project, Task upstreamTask, Task downstreamTask)
    void configureTestDependsOn(Project project, Task upstreamTask, Task downstreamTask)

    List<FileTree> testSourceSets(Project project)

    List<FileTree> sourceSets(Project project)
}