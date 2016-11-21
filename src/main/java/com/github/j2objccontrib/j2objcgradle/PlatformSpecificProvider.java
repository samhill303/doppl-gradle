package com.github.j2objccontrib.j2objcgradle;

import org.gradle.api.Project;
import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Created by kgalligan on 11/21/16.
 */
public interface PlatformSpecificProvider {
    HashSet<File> findGeneratedSourceDirs(Project project);
}
