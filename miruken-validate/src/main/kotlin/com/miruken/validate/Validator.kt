package com.miruken.validate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import kotlin.reflect.KType

class Validator : Handler(), Validating {
    override fun validate(
            target:     Any,
            targetType: KType?,
            scope:      Any?
    ): ValidationResult.Outcome {
        val composer   = COMPOSER
        val options    = composer?.getOptions(ValidationOptions())
        val validation = Validation(target, targetType, scope).apply {
            stopOnFailure = options?.stopOnFailure == true
        }
        composer?.handle(validation, true)
        return validation.outcome.also {
            (target as? ValidationAware)?.validationOutcome = it
        }
    }

    override fun validateAsync(
            target:     Any,
            targetType: KType?,
            scope:      Any?
    ): Promise<ValidationResult.Outcome> {
        val composer   = COMPOSER
        val options    = composer?.getOptions(ValidationOptions())
        val validation = Validation(target, targetType, scope).apply {
            wantsAsync = true
            stopOnFailure = options?.stopOnFailure == true
        }
        composer?.handle(validation, true)
        return (validation.result as Promise<*>) then {
            validation.outcome.also {
                (target as? ValidationAware)?.validationOutcome = it
            }
        }
    }
}