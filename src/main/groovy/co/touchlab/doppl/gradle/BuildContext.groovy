package co.touchlab.doppl.gradle

import co.touchlab.doppl.gradle.tasks.Utils
import org.gradle.api.Project

/**
 * This is a simple lazy wrapper around some classes that want to be shared but not during gradle init.
 * There's a better way to handle this, but to get things moving, we'll passively rely on lifecycle asking
 * for data at the right time.
 *
 * Created by kgalligan on 3/15/17.
 */
class BuildContext {
    private BuildTypeProvider buildTypeProvider;
    private DependencyResolver dependencyResolver;
    private final Project project;

    BuildContext(Project project) {
        this.project = project
    }

/**
     * Made synchronized in case we have tasks in parallel.
     *
     * @return
     */
    synchronized BuildTypeProvider getBuildTypeProvider() {
        if(buildTypeProvider == null) {
            boolean androidTypeProject = Utils.isAndroidTypeProject(project);
            buildTypeProvider = androidTypeProject ? new AndroidBuildTypeProvider(project) : new JavaBuildTypeProvider(project)
        }

        return buildTypeProvider
    }

    /**
     * Made synchronized in case we have tasks in parallel.
     *
     * @return
     */
    synchronized DependencyResolver getDependencyResolver() {
        if(dependencyResolver == null)
        {
            dependencyResolver = new DependencyResolver(project, DopplConfig.from(project))
            dependencyResolver.configureAll()
        }
        return dependencyResolver
    }
}
