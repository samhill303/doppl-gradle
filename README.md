# Doppl Gradle Plugin

This the Doppl framework Gradle plugin. Doppl is a build and dependency system
intended to facilitate Android and iOS code sharing using J2Ojbc.

To learn more about Doppl overall, check out the
[What is Doppl?](http://doppl.co/overview.html) and the
[Quick Start](http://doppl.co/docs/quicktutorial.html) pages on the
[main Doppl site](http://doppl.co).

## Adding the Plugin

In your project-level `build.gradle` file, you will need to add two lines to
your `buildscript` closure:

- `maven { url 'https://dl.bintray.com/doppllib/maven2' }` in the
`repositories` list

- `classpath 'co.doppl:gradle:0.9.0'` in the `dependnecies` roster

This will give you a `buildscript` akin to:

```groovy
buildscript {
    repositories {
        jcenter()
        maven { url 'https://dl.bintray.com/doppllib/maven2' }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.3'
        classpath 'co.doppl:gradle:0.9.0'
    }
}
```

Typically, you will also add `maven { url 'https://dl.bintray.com/doppllib/maven2' }`
to the `repositories` in the `allscripts` closure, so all of your modules will
be able to pull in Doppl-related runtime dependencies.

## Employing the Plugin

In the `build.gradle` file of each module that has code to be converted
by Doppl, add `apply plugin: 'co.doppl.gradle'`, typically towards the top of
the file. This will allow you to add a `dopplConfig` closure &mdash; as a peer
of your `android` closure &mdash; that configures Doppl's behavior. Plus, you
will be able to set up Doppl-specific dependencies.

See the [Doppl dependencies](docs/Dependencies)
and [Doppl configuration](docs/Configuration)
documentation for details on these.

## History

The Doppl plugin started as a fork of the earlier
[J2ObjC Gradle plugin](https://github.com/j2objc-contrib/j2objc-gradle) plugin.
Although ideologically similar, the two plugins differ significantly in the
details. Plus, at the present time, work on the J2ObjC Gradle plugin has been
suspended, while work on the Doppl Gradle plugin is still very active.

Many thanks to the
[developers](https://github.com/doppllib/doppl-gradle/blob/master/NOTICE#L19) of
the J2ObjC Gradle plugin.

### Main Contributors

* Advay Mengle @advayDev1 <source@madvay.com>
* Bruno Bowden @brunobowden <github@brunobowden.com>
* Michael Gorski @confile <mail@michaelgorski.de>

### Thanks

* Peter Niederweiser @pniederw <peter@pniederw.com>
* Sterling Greene @big-guy <sterling.greene@gradleware.com>

## Comparison to the J2ObjC Gradle Plugin

The J2ObjC Gradle plugin handles a lot of the dev process of J2objc, including compiling
and running tests, plus managing Xcode projects with CocoaPods. A lot of what
was removed were features better handled by other tools. The general design is
to do as little as is needed, and where better methods/tools are available, use
them. Doppl's Gradle plugin assembles dependencies and pushes code to J2objc, but
compiling Objective-C and linking apps is all done in Xcode. The idea is you are
going to have an easier time debugging Objective-C linker issues in Xcode than
using Gradle on the command line.

Here are some of the specific changes that were made to the J2ObjC Gradle plugin
when creating its Doppl equivalent.

### No Native Build

The J2ObjC Gradle plugin used Gradle's native build functionality to compile
and package libraries in the build step. In our experience, this could be
problematic, and when things failed, debugging was difficult. Native build, at
least in the way it was implemented, was cut off after Gradle 2.8, so the
original plugin was capped at that version. This was not workable going forward.
Doppl's Gradle plugin works fine through current Gradle versions, including
version 4 alpha.

### Off By Default

The J2ObjC Gradle plugin would run and rebuild your Objective-C whenever you ran
a build, which makes sense if you're always building that code. In practice,
generally you are editing Java while building Android, then running the
Objective-C conversion, and moving over to Xcode. To save time, the Doppl
Gradle plugin only runs when you specifically want to build the Objective-C
code, which is definitely not always (in practice).

### Android Compatible

You can have a standalone Java module, or run the Doppl Gradle plugin inside an
Android project. You will need to tell it what classes can and cannot be
transpiled, but you do not need to have a multi-module build.

### Dependencies

The ability to automatically fetch source JARs and transpile them has been
removed. Dependencies have a specific configuration and are built for Doppl by
the Gradle plugin. The source JAR download may be added back in the future, but
in general we think most libraries should at least have their tests run, and
setting up a separate project to build a library is not too difficult. You can
also simply copy the library source to your project, if you do not want to
bother making another library.

## License

This library is distributed under the Apache 2.0 license found in the [LICENSE](./LICENSE) file.
J2ObjC and libraries distributed with J2ObjC are under their own licenses.

