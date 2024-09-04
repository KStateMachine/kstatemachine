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

The library provides `metaInfo` property for `IState` and `Transition` types.
`MetaInfo` is a marker interface allowing to attach some static information to library primitives.
This mechanism is extendable and users may add their own `MetaInfo` sub interfaces/classes if necessary.
Currently, the only standard implementation is `UmlMetaInfo` which is useful for export feature.
See [controlling export output](https://kstatemachine.github.io/kstatemachine/pages/export.html#controlling-export-output).
You can build it using `buildUmlMetaInfo()` function.

{: .note }
`MetaInfo` considered to be immutable data by design