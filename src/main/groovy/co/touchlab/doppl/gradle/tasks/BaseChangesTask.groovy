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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input

/**
 * There are a lot of parameters that should pretty much always trigger a full rebuild, and trying to be smart about that
 * is probably not in anybody's best interest.
 *
 * Created by kgalligan on 3/21/17.
 */
class BaseChangesTask extends DefaultTask{

    BuildContext _buildContext

    @Input
    Map<String, String> getPrefixes() {
        DopplConfig.from(project).translatedPathPrefix
    }

    @Input
    String getJ2objcHome() { return Utils.j2objcHome(project) }

    @Input
    List<String> getTranslateArgs() {
        return DopplConfig.from(project).processedTranslateArgs()
    }

    @Input
    List<String> getTranslateJ2objcLibs() { return DopplConfig.from(project).translateJ2objcLibs }

    @Input boolean isEmitLineDirectives() {
        DopplConfig.from(project).emitLineDirectives
    }

    @Input boolean isDependenciesEmitLineDirectives() {
        DopplConfig.from(project).dependenciesEmitLineDirectives
    }
}
