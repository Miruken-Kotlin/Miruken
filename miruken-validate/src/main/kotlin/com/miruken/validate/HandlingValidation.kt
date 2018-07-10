package com.miruken.validate

import com.miruken.callback.Handling
import com.miruken.callback.aspectBefore
import com.miruken.callback.getOptions
import com.miruken.callback.handle
import com.miruken.concurrent.Promise
import com.miruken.typeOf
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun Handling.validate(
        target:        Any,
        targetType:    KType?,
        vararg scopes: KClass<*>
): ValidationResult.Outcome {
    val options    = getOptions(ValidationOptions())
    val validation = Validation(target, targetType, *scopes).apply {
        stopOnFailure = options?.stopOnFailure == true
    }
    handle(validation, true)
    return validation.outcome.also {
        (target as? ValidationAware)?.validationOutcome = it
    }
}

fun Handling.validateAsync(
        target:        Any,
        targetType:    KType?,
        vararg scopes: KClass<*>
): Promise<ValidationResult.Outcome> {
    val options    = getOptions(ValidationOptions())
    val validation = Validation(target, targetType, *scopes).apply {
        wantsAsync    = true
        stopOnFailure = options?.stopOnFailure == true
    }
    handle(validation, true)
    return (validation.result as Promise<*>) then {
        validation.outcome.also {
            (target as? ValidationAware)?.validationOutcome = it
        }
    }
}

inline fun <reified T: Any> Handling.validate(
        target:        T,
        vararg scopes: KClass<*>
) = validate(target, typeOf<T>(), *scopes)

inline fun <reified T: Any> Handling.validateAsync(
        target:        T,
        vararg scopes: KClass<*>
) = validateAsync(target, typeOf<T>(), *scopes)

inline fun <reified T: Any> Handling.valid(
        target:        T,
        vararg scopes: KClass<*>
) = aspectBefore({ _, composer ->
    composer.validate(target, *scopes).isValid
})

inline fun <reified T: Any> Handling.validAsync(
        target:        T,
        vararg scopes: KClass<*>
) = aspectBefore({ _, composer ->
    composer.validateAsync(target, *scopes) then {
        it.isValid
    }
})