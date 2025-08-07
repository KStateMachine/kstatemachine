---
layout: page
title: Multiplatform
---

# Multiplatform
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

## Supported platforms

* JVM
* Android
* iOS
* Native (Linux, MinGW, macOS)
* JS
* Wasm (WebAssembly)

_If you need missing platform support, please create a GitHub issue._

## Versioning

* Starting from **v0.22.0** KStateMachine has moved to Kotlin Multiplatform only with `JVM` platform support.
* In **v0.22.1** `iOS` support has been added
* **v0.30.0** adds `js` and `wasm` targets. `js` and `wasm` targets do not support blocking library APIs as those platforms do not have `runBlocking` support which
is used internally. 
* **v0.34.00** Adds native platforms support `linuxX64`, `mingwX64`, `macosX64` and `macosArm64`