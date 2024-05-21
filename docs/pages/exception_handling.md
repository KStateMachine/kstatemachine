---
layout: page
title: Exception handling
---

# Exception handling
{: .no_toc }

## Page contents
{: .no_toc .text-delta }

- TOC
{:toc}

## Exceptions from listeners

Event though `KStateMachine` assumes that listener callbacks should not throw exceptions, it may happen in practice.
If your app code throws exceptions in a listener callbacks library catches them, completes transition successfully and
passes the first occurred exception to `listenerExceptionHandler`. It simply rethrows exception by default, but you may
want to mute them with custom handler for example.

## Other exceptions

Exceptions coming from other client code callbacks, that are considered to be no-throwing (like guard functions of
transitions) are not caught. Machine will be automatically destroyed with `destroy()` function on such exceptions,
as it is in unpredictable state and cannot be used anymore.
Calling `processEvent()` on destroyed machine will throw also.
