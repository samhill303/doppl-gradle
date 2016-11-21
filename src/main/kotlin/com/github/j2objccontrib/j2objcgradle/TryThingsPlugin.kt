package com.github.j2objccontrib.j2objcgradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import java.io.File
import java.util.*

/**
 * Created by kgalligan on 11/21/16.
 */
class TryThingsPlugin : PlatformSpecificProvider {
    override fun findGeneratedSourceDirs(project: Project): HashSet<File> {
        val theFolders = HashSet<File>()
        project.plugins.all {
            when (it) {
                is AppPlugin -> {
                    println(it.toString())
                    val byType = project.extensions.getByType(AppExtension::class.java)
                    theFolders.addAll(configureAndroid(project,
                            byType.applicationVariants))
                }
                is LibraryPlugin -> configureAndroid(project,
                        project.extensions.getByType(LibraryExtension::class.java).libraryVariants)
            }
        }
        return theFolders
    }

    private fun <T : BaseVariant> configureAndroid(project: Project, variants: DomainObjectSet<T>): List<File> {
//        val generateSqlDelight = project.task("generateSqlDelightInterface")

        val compileDeps = project.configurations.getByName("compile").dependencies
        project.gradle.addListener(object : DependencyResolutionListener {
            override fun beforeResolve(dependencies: ResolvableDependencies?) {
                /*if (System.getProperty("sqldelight.skip.runtime") != "true") {
                    compileDeps.add(project.dependencies.create("com.squareup.sqldelight:runtime:$VERSION"))
                }*/
                compileDeps.add(
                        project.dependencies.create("com.android.support:support-annotations:23.1.1"))

                project.gradle.removeListener(this)
            }

            override fun afterResolve(dependencies: ResolvableDependencies?) { }
        })

        var rets = ArrayList<File>()
        variants.all {

            val taskName = "generate${it.name.capitalize()}SqlDelightInterface"

            val jc = it.javaClass
            val declaredField = jc.getDeclaredMethod("getVariantData")

            declaredField.isAccessible = true
            val variantData = declaredField.invoke(it) as BaseVariantData<*>
            val extraGeneratedSourceFolders = variantData.extraGeneratedSourceFolders
            rets.addAll(extraGeneratedSourceFolders)

            /*val task = project.tasks.create(taskName, SqlDelightTask::class.java)
            task.group = "sqldelight"
            task.buildDirectory = project.buildDir
            task.description = "Generate Android interfaces for working with ${it.name} database tables"
            task.source("src")
            task.include("**${File.separatorChar}*.$FILE_EXTENSION")

            generateSqlDelight.dependsOn(task)

            it.registerJavaGeneratingTask(task, task.outputDirectory)*/
        }

        return rets
    }
}