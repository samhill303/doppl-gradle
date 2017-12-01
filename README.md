# Doppl Gradle Plugin

This the Doppl framework Gradle plugin. Doppl is a build and dependency system intended to facilitate Android and iOS
code sharing using J2ojbc.

[What is Doppl?](http://doppl.co/overview.html)

[Quick Start](http://doppl.co/docs/quicktutorial.html)

[Doppl Gradle Plugin](http://doppl.co/docs/gradleplugin.html)

This plugin started as a fork of the [J2objc-gradle](https://github.com/j2objc-contrib/j2objc-gradle) project. It has morphed
significantly, but the general DNA in the translate pipeline is still there.

## Tasks

This is the task dependency tree. The referenced constants can be found in DopplPlugin.groovy. For users, the important 
ones to note are:

* TASK_DOPPL_BUILD - 'dopplBuild' This will stage and translate dependencies and source for both main and test paths,
and create the cocoapods podspec files to be used in Xcode. It's probably the only task you'll ever reference directly.

* TASK_DOPPL_ARCHIVE - 'dopplArchive' This creates the dependency structure for distributing libraries. You'll only need
this if you're creating a library.

* TASK_J2OBJC_PRE_BUILD - 'j2objcPreBuild' If you need to run something before EVERYTHING in doppl runs, make this task depend on your task.

* TASK_DOPPL_CONTEXT_BUILD - 'dopplContextBuild' Similar to above, but run after all the project's java operations run, 
including annotation processing.

![Task dependency tree](docs/dopplgradletree.png "Task dependency tree")

## License

This library is distributed under the Apache 2.0 license found in the [LICENSE](./LICENSE) file.
J2ObjC and libraries distributed with J2ObjC are under their own licenses.

