package co.touchlab.doppl.gradle.tasks

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
    List<String> getTranslateClasspaths() { return DopplConfig.from(project).translateClasspaths }

    @Input
    List<String> getTranslateJ2objcLibs() { return DopplConfig.from(project).translateJ2objcLibs }

    @Input
    boolean getIgnoreWeakAnnotations() { return DopplConfig.from(project).ignoreWeakAnnotations }

    @Input String mappingsInputPath() {
        DopplConfig.from(project).mappingsInput
    }
}
