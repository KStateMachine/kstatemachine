/*
 * Author: Mikhail Fedotov
 * Github: https://github.com/KStateMachine
 * Copyright (c) 2024.
 * All rights reserved.
 */

package ru.nsk.kstatemachine.persistence

import ru.nsk.kstatemachine.statemachine.StateMachine

fun interface RestorationResultValidator {
    /**
     * Throws if validation is not passed
     */
    fun validate(result: RestorationResult, recordedEvents: RecordedEvents, machine: StateMachine)
}

class RestorationResultValidationException(
    val result: RestorationResult,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Completely skips validation
 */
object EmptyValidator : RestorationResultValidator {
    override fun validate(result: RestorationResult, recordedEvents: RecordedEvents, machine: StateMachine) = Unit
}

/**
 * Does not allow warnings or failed processing results
 */
object StrictValidator : RestorationResultValidator {
    /**
     * @throws RestorationResultValidationException to indicate validation errors.
     */
    override fun validate(result: RestorationResult, recordedEvents: RecordedEvents, machine: StateMachine) {
        if (result.warnings.isNotEmpty())
            throw RestorationResultValidationException(
                result,
                "The ${RestorationResult::class.simpleName} contains warnings",
                result.warnings.first(),
            )
        result.results.forEach {
            if (it.warnings.isNotEmpty()) {
                throw RestorationResultValidationException(
                    result,
                    "The ${RestorationResult::class.simpleName} contains warnings",
                    it.warnings.first(),
                )
            } else if (it.processingResult.isFailure) {
                throw RestorationResultValidationException(
                    result,
                    "The ${RestorationResult::class.simpleName} contains failed processing result",
                    it.processingResult.exceptionOrNull(),
                )
            }
        }
    }
}