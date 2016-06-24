package com.github.j2objccontrib.j2objcgradle

/**
 * Created by kgalligan on 6/24/16.
 */
class DoppelDependency {
    String group
    String name
    String version

    DoppelDependency(String group, String name, String version) {
        this.group = group
        this.name = name
        this.version = version
    }

    String fullFolderName()
    {
        String foldername = group + "_" + name + "_" + version

        foldername = foldername.replace('-', '_')
        foldername = foldername.replace('.', '_')
        foldername = foldername.replace(' ', '_')

        return foldername
    }
}