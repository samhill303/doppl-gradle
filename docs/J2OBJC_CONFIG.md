# J2objc Config

The J2objc runtime needs to be configured correctly for the plugin to function. After J2objc 2.0.5, 
this Gradle plugin will run a stock J2objc installation. There are two ways to configure the Doppl plugin's J2objc
runtime:

+ Manual config: Download or build your own instance of J2objc.

+ Managed config: Tell the plugin what version to use and it will manage the download and install for you.

## Manual config

Until the next version of J2objc is released, the manual config option requires cloning and building J2objc 
from source. You'll need to build the "frameworks" version.

```
git clone https://github.com/google/j2objc.git

cd j2objc

make frameworks
```

Assuming you don't like waiting and have multiple cores, run the following (replace '8' with your number of 
cores):

```
make -j8 frameworks
```

When the build succeeds you should have a 'dist' dir. Get the full path to that dir. Add that dir to the file
'local.properties' in the root of your project. That file generally includes paths to the Android sdk and ndk.

```
sdk.dir=[your android sdk home]/sdk
j2objc.home=[the j2objc clone dir]/dist
```

If after setting the local j2objc value, if you are getting errors, check that the path is correct and the build 
finished successfully.

## Managed config

The gradle plugin can download and manage the J2objc runtime used for building the code. Add 'doppl_j2objc' to 
the 'gradle.properties' file.

```
doppl_j2objc=2.0.6a
```

When you run J2objc related tasks, if the local J2objc runtime is not installed, the plugin will download the 
J2objc runtime and install it in the user home directory.

**This will take a while**. It is about 350 megs.