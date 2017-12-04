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

package co.touchlab.doppl.gradle.analytics;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kgalligan on 5/10/17.
 */
public class AnalyticsPackage {
    String token;
    String distinct_id;
    boolean externalUser;

    @SerializedName("Anonymized MAC Address")
    String anonMacAddress;
    String j2objcVersion;
    String dopplVersion;

    @SerializedName("Host OS Type")
    String hostOs;

    @SerializedName("Host OS Version")
    String hostOsVersion;

    boolean emitLineDirectives = false;
    String targetVariant;
    boolean anyGeneratedSourceDirs;
    boolean anyGeneratedTestSourceDirs;
    boolean anyTranslateArgs;
//    boolean javaType;
//    boolean androidType;
}
