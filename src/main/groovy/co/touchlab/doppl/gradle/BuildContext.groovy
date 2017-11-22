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

import javax.annotation.Nonnull

/**
 * This is a simple lazy wrapper around some classes that want to be shared but not during gradle init.
 * There's a better way to handle this, but to get things moving, we'll passively rely on lifecycle asking
 * for data at the right time.
 *
 * Created by kgalligan on 3/15/17.
 */
class BuildContext {
    private BuildTypeProvider buildTypeProvider;
    private final DependencyResolver dependencyResolver;
    private final Project project;

    BuildContext(Project project, DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver
        this.project = project
    }

    /**
     * Made synchronized in case we have tasks in parallel.
     *
     * @return
     */
    @Nonnull
    synchronized BuildTypeProvider getBuildTypeProvider() {
        if(buildTypeProvider == null)
        {
            boolean androidTypeProject = Utils.isAndroidTypeProject(project);
            this.buildTypeProvider = androidTypeProject ? new AndroidBuildTypeProvider(project) : new JavaBuildTypeProvider(project)
        }
        return buildTypeProvider
    }

    /**
     * Made synchronized in case we have tasks in parallel.
     *
     * @return
     */
    synchronized DependencyResolver getDependencyResolver() {
        return dependencyResolver
    }
}
