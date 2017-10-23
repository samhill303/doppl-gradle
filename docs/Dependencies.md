# Doppl, Gradle, and Dependencies

Most likely, your Android app is using dependencies, whether they are from
Google (e.g., Android Support Library) or from third parties (e.g., Retrofit).

With Doppl, dependencies fall into two categories:

- Those that are out of scope for Doppl, because they are tied to the UI

- Everything else

For that latter category, the Doppl team maintains a set of libraries that you
can add to your project that will be used in Doppl builds as alternatives to
the regular library that you might be using. These get added to your
`dependencies` closure via `doppl` and `testDoppl` statements.

## Adding Doppl Dependencies

`doppl` and `testDoppl` are configurations, just like `compile` and `testCompile`,
where you then provide a library that has been configured to work with Doppl.

```groovy
doppl 'co.doppl.lib:androidbase:0.7.4.0'
testDoppl 'co.doppl.lib:androidbasetest:0.7.4.0'
```

Frequently, `doppl` configurations are paired with `compile` configurations,
where the `compile` configuration points to a standard Android dependency and
the `doppl` configuration points to its `doppl` equivalent:

```groovy
compile 'de.greenrobot:eventbus:2.4.0'
doppl 'co.doppl.de.greenrobot:eventbus:2.4.0.1'
```

## What Doppl Dependencies Are Available?

The libraries we have published tend to be prefixed with `co.doppl`, even when a
fork of another public library, and have an extra number to their version. The
extra number allows multiple releases tagged to a base version, but also it is
there because Maven cannot really handle two artifacts at one location.

The Doppl Web site has
[a list of Doppl-maintained libraries](https://github.com/doppllib/doppllib.github.io/blob/master/docs/Libraries.md)
that you can use.

## What Does Not Need to be Doppl'd

You only need to worry about Doppl dependencies for libraries that are used
directly by the code that you want to convert for use on iOS. Conversely,
any code that is tied inextricably to the UI do not need to have `doppl`
equivalents. This includes both Google libraries (e.g., `recyclerview-v7`)
and third-party libraries (e.g., ButterKnife).

Similarly, libraries that are tied to Android-specific concerns &mdash; such
as wrapper libraries around fingerprint authentication &mdash;
would not need to be included.

## Requiring the Doppl Runtime

If your app is not using *any* of the other Doppl dependencies, you will need to
add one for the runtime:

```groovy
doppl 'co.doppl.lib:androidbase:0.8.5.0'
```

(where the version number will change from time to time)

This allows you to use the Android SDK classes outlined in
[the `core-doppl` repo](https://github.com/doppllib/core-doppl).

For testing, you will want to use:

```groovy
testCompile 'co.doppl.lib:androidbasetest:0.8.5'
testDoppl 'co.doppl.lib:androidbasetest:0.8.5.0'
```

(where the version number will change from time to time)

This allows you to write tests using the test classes outlined in
[the `core-doppl` repo](https://github.com/doppllib/core-doppl).

## What's In a Doppl Dependency?

The archive is zip format with the extension 'dop', which contains the
Objective-C code, the JAR (`j2objc` needs to read the Java types), and a few
config files.

**NOTE**: We keep Objective-C in this archive, but we'll ~~probably~~ need to
change that in the future. If/when J2Objc metadata changes, old builds will not
be compatible. There is no solid technical reason to keep Objective-C vs. the
source Java itself, except (arguably) performance, but this is minimal.

