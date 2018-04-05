package com.miruken.validate

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import kotlin.reflect.KClass
import kotlin.reflect.KType

class Validator : Handler(), Validating {
    override fun validate(
            target:        Any,
            targetType:    KType?,
            vararg scopes: KClass<*>
    ) = requireComposer().run {
            val options    = getOptions(ValidationOptions())
            val validation = Validation(target, targetType, *scopes).apply {
                stopOnFailure = options?.stopOnFailure == true
            }
            handle(validation, true)
            validation.outcome.also {
                (target as? ValidationAware)?.validationOutcome = it
            }
        }

    override fun validateAsync(
            target:        Any,
            targetType:    KType?,
            vararg scopes: KClass<*>
    ) = requireComposer().run {
            val options    = getOptions(ValidationOptions())
            val validation = Validation(target, targetType, *scopes).apply {
                wantsAsync = true
                stopOnFailure = options?.stopOnFailure == true
            }
            handle(validation, true)
            (validation.result as Promise<*>) then {
                validation.outcome.also {
                    (target as? ValidationAware)?.validationOutcome = it
                }
            }
        }
}