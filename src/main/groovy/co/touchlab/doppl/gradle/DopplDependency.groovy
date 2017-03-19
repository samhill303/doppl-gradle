package co.touchlab.doppl.gradle

/**
 * Created by kgalligan on 6/24/16.
 */
class DopplDependency {
    String name
    String versionMame
    File dir

    DopplDependency(String name, File dir) {
        this.name = name
        this.versionMame = name
        this.dir = dir
    }

    DopplDependency(String group, String name, String version, File explodedDir)
    {
        this.name = name

        String foldername = group + "_" + name + "_" + version

        this.versionMame = foldername

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

        DopplDependency that = (DopplDependency) o

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