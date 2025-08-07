---
layout: page
title: Install
---

# Install
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

KStateMachine is available on `Maven Central` and `JitPack` repositories.

The library consists of the fallowing components (artifacts):

* `kstatemachine` - (mandatory) state machine implementation (depends only on **Kotlin Standard library**)
* `kstatemachine-coroutines` - (optional) add-ons for working with coroutines (depends on **Kotlin Coroutines library**)
* `kstatemachine-serialization` - (optional) add-ons for serialization (depends on **Kotlin Serialization library**). 
  Released in `v0.32.0`

Please note that starting from `v0.22.0` the library switched to **Kotlin Multiplatform** and artifact naming has changed.

## Maven Central

Add dependencies:

```kotlin
// kotlin
dependencies {
    // multiplatform artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization:<Tag>")
    // or JVM/Android artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine-jvm:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-jvm:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-jvm:<Tag>")
    // or iOS artifacts (starting from 0.22.1)
    implementation("io.github.nsk90:kstatemachine-iosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-iosarm64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-iosx64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iossimulatorarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iossimulatorarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-iossimulatorarm64:<Tag>")
    // or linux
    implementation("io.github.nsk90:kstatemachine-linuxx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-linuxx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-linuxx64:<Tag>")  
    // or mingw
    implementation("io.github.nsk90:kstatemachine-mingwx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-mingwx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-mingwx64:<Tag>") 
    // or macos 
    implementation("io.github.nsk90:kstatemachine-macosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-macosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-macosx64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-macosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-macosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-macosarm64:<Tag>")
    // or JS
    implementation("io.github.nsk90:kstatemachine-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-js:<Tag>")
    // or WebAssembly (Wasm)
    implementation("io.github.nsk90:kstatemachine-wasm-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-wasm-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-serialization-wasm-js:<Tag>")
}
```

```groovy
// groovy
dependencies {
    // multiplatform artifacts
    implementation 'io.github.nsk90:kstatemachine:<Tag>'
    implementation 'io.github.nsk90:kstatemachine-coroutines:<Tag>' // optional
    implementation 'io.github.nsk90:kstatemachine-serialization:<Tag>' // optional
    // etc..
}
```

Where `<Tag>` is a library version.

You can see official docs
about [dependencies on multiplatform libraries](https://kotlinlang.org/docs/multiplatform-add-dependencies.html#library-used-in-specific-source-sets)

## JitPack 
Deprecated
{: .label .label-red }

Currently, `JitPack` does not support Kotlin multiplatform artifacts.
So versions starting from `0.22.0` are not available there, use `Maven Central` instead.

Add the [JitPack](https://jitpack.io/#nsk90/kstatemachine/Tag) repository to your build file. Add it in your
root `build.gradle` at the end of repositories:

```kotlin
// kotlin
repositories {
    //  ...
    maven { url = uri("https://jitpack.io") }
}
```

```groovy
// groovy
allprojects {
    repositories {
        //  ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add dependencies:

```kotlin
// kotlin
dependencies {
    implementation("com.github.nsk90:kstatemachine:<Tag>")
    // note that group is different in second artifact, long group name also works for first artifact but not vise versa
    // it is some strange JitPack behaviour
    implementation("com.github.nsk90.kstatemachine:kstatemachine-coroutines:<Tag>") // optional
    implementation("com.github.nsk90.kstatemachine:kstatemachine-serialization:<Tag>") // optional
}
```

```groovy
// groovy
dependencies {
    implementation 'com.github.nsk90:kstatemachine:<Tag>'
    // note that group is different in second artifact, long group name also works for first artifact but not vise versa
    // it is some strange JitPack behaviour
    implementation 'com.github.nsk90.kstatemachine:kstatemachine-coroutines:<Tag>' // optional
    implementation 'com.github.nsk90.kstatemachine:kstatemachine-serialization:<Tag>' // optional
}
```

Where `<Tag>` is a library version.