package com.miruken.validate

interface ValidationAware {
    var validationOutcome: ValidationResult.Outcome?
}