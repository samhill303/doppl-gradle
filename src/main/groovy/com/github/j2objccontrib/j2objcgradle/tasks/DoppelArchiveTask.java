package com.github.j2objccontrib.j2objcgradle.tasks;


import com.github.j2objccontrib.j2objcgradle.J2objcConfig;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.bundling.Zip;

/**
 * Created by kgalligan on 6/22/16.
 */
public class DoppelArchiveTask extends Zip {


    public DoppelArchiveTask() {
//        setClassifier("dop");
        setExtension("dop");
        String doppelDeployDirectory = J2objcConfig.from(getProject()).getDoppelDeployDirectory();
        from(inputDirectory(doppelDeployDirectory));
//        into(doppelDeployDirectory);
    }

    @InputDirectory
    String inputDirectory(String doppelDeployDirectory) {
        return doppelDeployDirectory + "/" + getProject().getName();
    }


}
