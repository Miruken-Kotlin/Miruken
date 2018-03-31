package com.miruken.validate

class ValidationException(
        val outcome: ValidationResult.Outcome,
        message:     String
): Exception(message) {
    constructor(outcome: ValidationResult.Outcome)
            : this(outcome, outcome.error)
}