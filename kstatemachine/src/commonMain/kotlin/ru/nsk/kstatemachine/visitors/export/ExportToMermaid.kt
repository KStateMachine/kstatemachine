/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.visitors.export

import ru.nsk.kstatemachine.metainfo.ExportMetaInfo
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.visitors.export.CompatibilityFormat.MERMAID

/**
 * Export [StateMachine] to Mermaid state diagram
 * @see <a href="https://mermaid.js.org/">Mermaid</a>
 *
 * [showEventLabels] prints event types for transitions
 * [unsafeCallConditionalLambdas] will call conditional lambdas which can touch application data,
 * this may give more complete output, but may be not safe ([ClassCastException] may be thrown).
 * See [ExportMetaInfo] for more info.
 */
suspend fun StateMachine.exportToMermaid(
    showEventLabels: Boolean = false,
    unsafeCallConditionalLambdas: Boolean = false,
) = with(ExportPlantUmlVisitor(MERMAID, showEventLabels, unsafeCallConditionalLambdas)) {
    accept(this)
    export()
}

/** Blocking analog for [exportToMermaid] */
fun StateMachine.exportToMermaidBlocking(
    showEventLabels: Boolean = false,
    unsafeCallConditionalLambdas: Boolean = false,
) = coroutineAbstraction.runBlocking {
    with(ExportPlantUmlVisitor(MERMAID, showEventLabels, unsafeCallConditionalLambdas)) {
        accept(this)
        export()
    }
}