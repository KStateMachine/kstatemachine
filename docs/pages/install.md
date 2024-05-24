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

The library consists of 2 components:

* `kstatemachine` - (mandatory) state machine implementation (depends only on Kotlin Standard library)
* `kstatemachine-coroutines` - (optional) add-ons for working with coroutines (depends on Kotlin Coroutines library)

Please note that starting from `v0.22.0` the library switched to Kotlin Multiplatform and artifact naming has changed.

## Maven Central

Add dependencies:

```kotlin
// kotlin
dependencies {
    // multiplatform artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines:<Tag>")
    // or JVM/Android artifacts (starting from 0.22.0)
    implementation("io.github.nsk90:kstatemachine-jvm:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-jvm:<Tag>")
    // or iOS artifacts (starting from 0.22.1)
    implementation("io.github.nsk90:kstatemachine-iosarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosarm64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iosx64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iosx64:<Tag>")

    implementation("io.github.nsk90:kstatemachine-iossimulatorarm64:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-iossimulatorarm64:<Tag>")
    // or JS
    implementation("io.github.nsk90:kstatemachine-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-js:<Tag>")
    // or WebAssembly (Wasm)
    implementation("io.github.nsk90:kstatemachine-wasm-js:<Tag>")
    implementation("io.github.nsk90:kstatemachine-coroutines-wasm-js:<Tag>")
}
```

```groovy
// groovy
dependencies {
    // multiplatform artifacts
    implementation 'io.github.nsk90:kstatemachine:<Tag>'
    implementation 'io.github.nsk90:kstatemachine-coroutines:<Tag>' // optional
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
}
```

```groovy
// groovy
dependencies {
    implementation 'com.github.nsk90:kstatemachine:<Tag>'
    // note that group is different in second artifact, long group name also works for first artifact but not vise versa
    // it is some strange JitPack behaviour
    implementation 'com.github.nsk90.kstatemachine:kstatemachine-coroutines:<Tag>' // optional
}
```

Where `<Tag>` is a library version.