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

    boolean useArc = false;
    boolean anyMainOutputs;
    boolean anyTestOutputs;
    boolean copyDependencies = false;
    boolean emitLineDirectives = false;
    String targetVariant;
    boolean anyGeneratedSourceDirs;
    boolean anyGeneratedTestSourceDirs;
    boolean anyOverlaySourceDirs;
    boolean anyTranslateArgs;
    boolean anyTranslateClasspaths;
//    boolean javaType;
//    boolean androidType;
}
