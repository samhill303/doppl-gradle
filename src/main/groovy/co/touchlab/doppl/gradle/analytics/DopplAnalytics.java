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

import co.touchlab.doppl.gradle.DopplConfig;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Set;

/**
 * Created by kgalligan on 4/7/17.
 */
public class DopplAnalytics {
    private static DopplAnalytics instance;
    private static final int READ_TIMEOUT = 2000;
    private static final int CONNECT_TIMEOUT = 4000;
    private static final String ADDRESS_PREFIX = "https://api.mixpanel.com/track/?data=";
    private static final String ADDRESS_SUFFIX = "&ip=1";
    private static final String TOKEN = "a6b86f2bfca54adc8a9a5dbe28d615b5";
    private static final String EVENT_NAME = "Translate";

    public static class JsonWrapper
    {
        String event = EVENT_NAME;
        AnalyticsPackage properties;
    }

    // The list of packages the model classes reside in
    private Set<String> packages;
    private DopplConfig config;
    String j2objcVersion;

    public DopplAnalytics(DopplConfig config,
                          String j2objcVersion) {
        this.config = config;
        this.j2objcVersion = j2objcVersion;
    }

    private void send() {
        try {
            URL url = getUrl();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            connection.getResponseCode();
        } catch (Exception ignored) {
        }
    }

    public void execute() {
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                send();
            }
        });
        backgroundThread.start();
        try {
            backgroundThread.join(CONNECT_TIMEOUT + READ_TIMEOUT);
        } catch (InterruptedException ignored) {
            // We ignore this exception on purpose not to break the build system if this class fails
        } catch (IllegalArgumentException ignored) {
            // We ignore this exception on purpose not to break the build system if this class fails
        }
    }

    public URL getUrl() throws
            MalformedURLException,
            SocketException,
            NoSuchAlgorithmException,
            UnsupportedEncodingException {
        return new URL(ADDRESS_PREFIX + Utils.base64Encode(generateJson()) + ADDRESS_SUFFIX);
    }

    public String generateJson() throws SocketException, NoSuchAlgorithmException {

        AnalyticsPackage analyticsPackage = new AnalyticsPackage();
        analyticsPackage.token = TOKEN;
        analyticsPackage.distinct_id = ComputerIdentifierGenerator.get();
        analyticsPackage.externalUser = false;
        analyticsPackage.anonMacAddress = analyticsPackage.distinct_id;
        analyticsPackage.j2objcVersion = this.j2objcVersion;
        analyticsPackage.dopplVersion = findMyVersion();
        analyticsPackage.hostOs = System.getProperty("os.name");
        analyticsPackage.hostOsVersion = System.getProperty("os.version");
        analyticsPackage.useArc = config.getUseArc();
        analyticsPackage.emitLineDirectives = config.getEmitLineDirectives();
        analyticsPackage.targetVariant = config.getTargetVariant();
        analyticsPackage.anyGeneratedSourceDirs = !config.getGeneratedSourceDirs().isEmpty();
        analyticsPackage.anyGeneratedTestSourceDirs = !config.getGeneratedTestSourceDirs().isEmpty();
        analyticsPackage.anyTranslateArgs = !config.getTranslateArgs().isEmpty();

        JsonWrapper jsonWrapper = new JsonWrapper();
        jsonWrapper.properties = analyticsPackage;

        return new Gson().toJson(jsonWrapper);
    }

    String findMyVersion()
    {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("/buildgen.properties");
            Properties buildgen = new Properties();
            buildgen.load(resourceAsStream);
            resourceAsStream.close();
            return buildgen.getProperty("buildversion");
        } catch (IOException e) {
            return "(failed load)";
        }
    }
}
