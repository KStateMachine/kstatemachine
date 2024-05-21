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
* JS
* Wasm (WebAssembly)

_If you need missing platform support, please create a GitHub issue._

## Versioning

Starting from v0.22.0 KStateMachine has moved to Kotlin Multiplatform only with `JVM` platform support.
In **v0.22.1** `iOS` support has been added also, **v0.30.0** adds `js` and `wasm` targets.
`js` and `wasm` targets do not support blocking library APIs as those platforms do not have `runBlocking` support which
is used internally.