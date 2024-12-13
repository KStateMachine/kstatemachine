---
layout: page
title: Meta information
---

# Meta information
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

## `metaInfo` property

The library declares `metaInfo` property for `IState` and `Transition` types.
`MetaInfo` is a marker interface allowing to attach some static information to library primitives.
This mechanism is extendable and users may add their own `MetaInfo` sub interfaces/classes if necessary.

{: .note }
`MetaInfo` considered to be immutable data by design

## `MetaInfo` composition 

If you need to specify more than one `MetaInfo` instance for `IState` or `Transition` you have two options:
1) Use `CompositeMetaInfo` which is constructed by `buildCompositeMetaInfo` builder function. 
This composite type allows to specify a set of `MetaInfo` objects.

   Limitations:
   * `CompositeMetaInfo` cannot nest into each other. Only one layer is supported.
   * Don't try to specify `MetaInfo` of same type multiple times. This is wrong by design. 
     Certain `MetaInfo` subtype should be applied only once. Exception will be thrown otherwise.
2) Manually implement all required `MetaInfo` interfaces in a single object.

Both options are supported, choose any one you like more.

## Built-in `MetaInfo` subclasses

* One of standard implementations is `UmlMetaInfo` which is useful for export feature.
See [controlling export output](https://kstatemachine.github.io/kstatemachine/pages/export.html#controlling-export-output).
You can build it using `buildUmlMetaInfo()` function.
* `IgnoreUnsafeCallConditionalLambdasMetaInfo` and `ExportMetaInfo` are also used by export feature.
See [controlling export output](https://kstatemachine.github.io/kstatemachine/pages/export.html#controlling-export-output).