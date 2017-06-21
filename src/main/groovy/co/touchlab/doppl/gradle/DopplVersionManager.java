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

package co.touchlab.doppl.gradle;

import co.touchlab.doppl.gradle.tasks.Utils;
import org.gradle.api.Project;

import java.io.File;

/**
 * Created by kgalligan on 1/19/17.
 */
public class DopplVersionManager {

    public static final String EXPLICIT_J2OBJC_VERSION = "1.3.2a-doppl";

    public static void verifyJ2objcRequirements(Project project) {
        String j2objcHome = Utils.j2objcHomeOrNull(project);

        if (j2objcHome == null) {
            Utils.throwJ2objcConfigFailure("J2ObjC Home not set. Please check local.properties to make sure your j2objc.home is set.");
        }

        File j2objcHomeFile = new File(j2objcHome);

        if (!j2objcHomeFile.exists()) {
            Utils.throwJ2objcConfigFailure("J2ObjC Home directory not found, expected at " + j2objcHome);
        }

        // Verify that underlying J2ObjC binary exists at all.
        File j2objcJar = Utils.j2objcJar(project);
        if (!j2objcJar.exists()) {
            Utils.throwJ2objcConfigFailure("J2ObjC binary does not exist at " + j2objcJar.getAbsolutePath());
        }

        checkJ2objcVersion(project, EXPLICIT_J2OBJC_VERSION);
    }

    private static void checkJ2objcVersion(Project project, String explicitJ2objcVersion) {
        String foundJ2objcVersion = Utils.findVersionString(project, Utils.j2objcHome(project));
        /*if (!foundJ2objcVersion.equals(explicitJ2objcVersion)) {
            // Note that actualVersionString will usually already have the word 'j2objc' in it.
            Utils.throwJ2objcConfigFailure(project,
                    "Found $foundJ2objcVersion at $j2objcHome, J2ObjC v$explicitJ2objcVersion required.");
        }*/
    }
}
