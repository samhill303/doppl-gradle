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
                                        val byType = project.extensions.getByType(AppExtension::class.java)
                                        theFolders.addAll(configureAndroid(
                                                byType.applicationVariants))

//                    theFolders.addAll(configureAndroid(project.extensions.getByType(AppExtension::class.java).applicationVariants))
                }
                is LibraryPlugin -> {
                    theFolders.addAll(configureAndroid(project.extensions.getByType(LibraryExtension::class.java).libraryVariants))
                }
            }
        }
        return theFolders
    }

    private fun <T : BaseVariant> configureAndroid(variants: DomainObjectSet<T>): List<File> {
        var rets = ArrayList<File>()
        variants.all {

//            val testDirs = ArrayList<File>()
//
//            it.sourceSets.all {
//                testDirs.addAll(it.javaDirectories)
//            }

            val jc = it.javaClass
            val declaredField = jc.getDeclaredMethod("getVariantData")

            declaredField.isAccessible = true
            val variantData = declaredField.invoke(it) as BaseVariantData<*>
            val extraGeneratedSourceFolders = variantData.extraGeneratedSourceFolders
            if(extraGeneratedSourceFolders != null)
                rets.addAll(extraGeneratedSourceFolders)
        }

        return rets
    }
}