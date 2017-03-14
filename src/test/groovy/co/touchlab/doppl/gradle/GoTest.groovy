package co.touchlab.doppl.gradle

import co.touchlab.doppl.gradle.tasks.TestingUtils
import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.Project
import org.junit.Before

/**
 * Created by kgalligan on 3/13/17.
 */
class GoTest {

    private Project proj
    private String j2objcHome
    private DopplConfig j2objcConfig

    @Before
    void setUp() {
        // Default to native OS except for specific tests
        Utils.setFakeOSNone()
        (proj, j2objcHome, j2objcConfig) = TestingUtils.setupProject(
                new TestingUtils.ProjectConfig(applyJavaPlugin: true, createJ2objcConfig: true))

        // The files in these folders are created by the 'genNonIncrementalInputs' method
        proj.file('src/main/java/com/example').mkdirs()
        proj.file('src/test/java/com/example').mkdirs()
    }

}
