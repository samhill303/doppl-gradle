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

import co.touchlab.doppl.gradle.helper.J2objcRuntimeHelper;
import co.touchlab.doppl.gradle.tasks.Utils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;

/**
 * Created by kgalligan on 1/19/17.
 */
public class DopplVersionManager {

    public static final String J2OBJC_CONFIG_MESSAGE = "J2objc config not complete. See: https://github.com/doppllib/doppl-gradle/blob/master/docs/J2OBJC_CONFIG.md";

    public static void checkJ2objcConfig(Project project, boolean forceDownload) {

        String version = Utils.j2objcDeclaredVersion(project);

        if (version == null) {
            //Check local.properties
            String j2objcHome = Utils.j2objcLocalHomeOrNull(project);
            checkJ2objcValid(project, j2objcHome);
        } else if (forceDownload) {
            try {
                File managedJ2objc = J2objcRuntimeHelper.checkAndDownload(project, version);
                checkJ2objcValid(project, managedJ2objc.getCanonicalPath());
            } catch (IOException e) {
                throwJ2objcConfigFailure("J2ObjC runtime download failed " +
                        "\n\n" + J2OBJC_CONFIG_MESSAGE, e);
            }
        }
    }

    public static void verifyJ2objcRequirements(Project project) {
        String j2objcHome = Utils.j2objcLocalHomeOrNull(project);

        checkJ2objcValid(project, j2objcHome);
    }

    private static void checkJ2objcValid(Project project, String j2objcHome) {
        if (j2objcHome == null) {
            throwJ2objcConfigFailure(J2OBJC_CONFIG_MESSAGE, null);
        }

        File j2objcHomeFile = new File(j2objcHome);

        if (!j2objcHomeFile.exists()) {
            throwJ2objcConfigFailure("J2ObjC Home directory not found, expected at " +
                    j2objcHome +"\n\n"+ J2OBJC_CONFIG_MESSAGE, null);
        }

        // Verify that underlying J2ObjC binary exists at all.
        File j2objcExecutable = new File(j2objcHomeFile, "j2objc");
        if (!j2objcExecutable.exists()) {
            throwJ2objcConfigFailure("J2ObjC binary does not exist at " +
                    j2objcExecutable.getAbsolutePath() +"\n\n"+ J2OBJC_CONFIG_MESSAGE, null);
        }
    }

    private static void throwJ2objcConfigFailure(String preamble, Throwable cause) {
        String message = ">>>>>>>>>>>>>>>> Doppl Tool Configuration Error <<<<<<<<<<<<<<<<\n" +
                preamble + "\n" +
                "\n" +
                "See 'Getting Started' at http://doppl.co\n";
        if(cause == null)
            throw new InvalidUserDataException(message);
        else
            throw new InvalidUserDataException(message, cause);
    }

    private static void checkJ2objcVersion(Project project, String explicitJ2objcVersion) {
//        String foundJ2objcVersion = Utils.findVersionString(project, Utils.j2objcHome(project));
        /*if (!foundJ2objcVersion.equals(explicitJ2objcVersion)) {
            // Note that actualVersionString will usually already have the word 'j2objc' in it.
            Utils.throwJ2objcConfigFailure(project,
                    "Found $foundJ2objcVersion at $j2objcHome, J2ObjC v$explicitJ2objcVersion required.");
        }*/
    }
}
