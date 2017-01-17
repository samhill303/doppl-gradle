package com.github.j2objccontrib.j2objcgradle

/**
 * Created by kgalligan on 6/24/16.
 */
class DoppelDependency {
    String name
    File dir

    DoppelDependency(String name, File dir) {
        this.name = name
        this.dir = dir
    }

    DoppelDependency(String group, String name, String version, File explodedDir)
    {
        this.name = name

        String foldername = group + "_" + name + "_" + version

        foldername = foldername.replace('-', '_')
        foldername = foldername.replace('.', '_')
        foldername = foldername.replace(' ', '_')

        dir = new File(explodedDir, foldername)
    }

    File dependencyFolderLocation(){
        return dir
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DoppelDependency that = (DoppelDependency) o

        if (dir != that.dir) return false
        if (name != that.name) return false

        return true
    }

    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (dir != null ? dir.hashCode() : 0)
        return result
    }
}