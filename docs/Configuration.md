# Doppl Configuration

Other than the [dependencies](./Dependencies), most of your Doppl configuration
will come in the form of statements inside of the `dopplConfig` closure.
This, like the `android` closure, configures a Gradle plugin, in this case the
Doppl plugin.

## Input Options

The Doppl Gradle plugin has a few configuration options for
controlling what should be converted from your app, most importantly the
`translatePattern` option. 

### `translatePattern` and `testIdentifier`

Doppl needs to know which Java files in your app need to be translated. By default,
it will translate *all* Java files. While this may be appropriate for some
libraries, it is unlikely to be the right answer for an app, as an app usually
has some UI code that Doppl should ignore.

To that end, `translatePattern` is a closure where you build up a file set of
what should be translated. If you specify `translatePattern`, Doppl will
translate *no* Java files by default, except for what the `translatePattern`
closure specifies. In that closure, you can have `include` and
`exclude` statements to provide file glob specifications for what should be
included and excluded from the file set:

```groovy
translatePattern {
    include '**/data/**'
    include '**/presenter/**'
    include '**/test/**'
    include '**/AppManager.java'
}
```

Here, we include Java classes that have `data`, `presenter`, or `test` in their
paths, plus `AppManager.java`.

`testIdentifier` provides a similar closure where you can build up a file set
of the tests that should be part of the conversion. The default, if you do not
provide `testIdentifier`, is to use all classes ending in `Test.java` (e.g.,
`FooTest.java`). More than likely, you will want to provide your own `include`
or `exclude` rules in your own `testIdentifier` closure.

**NOTE**: Test classes must be covered in *both* `translatePattern` (to translate
the code) and in `testIdentifier` (to find the tests and add them to a list of
tests to run in Xcode).

### `generatedSourceDirs` and `generatedTestSourceDirs`

You may be using other Gradle plugins, or features of those plugins, that
generate Java code. Usually, the Doppl Gradle plugin can find such generated
code automatically. Occasionally, it cannot, at which point it will need your
help, in the form of two configuration options:

- `generatedSourceDirs`: add generated source files directories (e.g. files
created from Dagger annotations)

- `generatedTestSourceDirs`: add generated source files directories (e.g. files
created from Dagger annotations) in tests

```groovy
generatedSourceDirs 'build/generated/source/apt/main'
```

## Output Options

You can also configure how Doppl generates the resulting Objective-C code.

### `translatedPathPrefix` 

By default, Doppl generates Objective-C class names based upon the fully-qualified
Java class name. However, this leads to very verbose Objective-C class names.
For example, a Java class named `Home` in `co.touchlab.droidcon.android.shared.data`
will be translated into a `CoTouchlabDroidconAndroidSharedDataHome` Objective-C
class.

To help maintain your sanity, you can use one or more `translatedPathPrefix`
statements, to map a Java package to a unique shorthand prefix to use for the
Objective-C class.

```groovy
translatedPathPrefix 'co.touchlab.droidcon.android.shared.data', 'DCD'
```
Now, instead of having your Objective-C class be `CoTouchlabDroidconAndroidSharedDataHome`,
it will be shortened to `DCDHome`.

## Advanced Options

These options are here for Xcode experts, particularly those who do not wish
to use Cocoapods as the means by which Doppl publishes the translated code for
use in an iOS app.

### `copyMainOutput` and `copyTestOutput`

`copyMainOutput` specifies, relative to the module directory, where the
generated Objective-C code should go, for the code from your `main/` source set. 

`copyMainOutput`: The output path for Objective-C files. The default, if you
do not specify it, is `build/j2objcSrcGenMain/`.

```groovy
copyMainOutput '../ios/scratchllframework/main'
```

Similarly, `copyTestOutput` specifies, relative to the module directory, where
the generated Objective-C code should go, for the code from your `test/` source
set for JUnit-based unit tests. The default is  `build/j2objcSrcGenTest/`.

## Miscellaneous Options

The Doppl Gradle plugin also has a variety of additional options that you can
configure:

- `disableAnalytics`: Boolean; whether you want Analytics turned on. Defaults to
`false`. To see what data Doppl collects, see
[this document](https://github.com/doppllib/doppl-gradle/blob/master/src/main/groovy/co/touchlab/doppl/gradle/analytics/DopplAnalytics.java).

- `emitLineDirectives`: Boolean; generates debugging support. Defaults to `false`.
For more information on debugging, see our [document on debugging](./debugging.html).

- `translateArgs`: Used to add different options to the way your Java code is
translated. The list of supported options can be found
[here](https://developers.google.com/j2objc/reference/j2objc).
`TranslateTask` already uses some of these args, so they do not have to be added
manually. To see the arguments being added, you can
[look at the source here](https://github.com/doppllib/doppl-gradle/blob/master/src/main/groovy/co/touchlab/doppl/gradle/tasks/TranslateTask.groovy#L332).

- `skipDependsTasks`: Boolean; Skips the need to depend on `test`, `jar`, and
`javaCompile` tasks. Defaults to `false`.

## Unsupported Legacy Options

If you see sigs of a `cycleFinderArgs` configuration option in the plugin source
code, ignore it. CyclerFinder in general is not currently supported by Doppl.
