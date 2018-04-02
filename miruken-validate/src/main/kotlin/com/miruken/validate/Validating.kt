package com.miruken.validate

import com.miruken.callback.Handling
import com.miruken.callback.aspectBefore
import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf
import kotlin.reflect.KType

@Protocol
interface Validating {
    fun validate(
            target:     Any,
            targetType: KType,
            scope:      Any? = null
    ): ValidationResult.Outcome

    fun validateAsync(
            target:     Any,
            targetType: KType,
            scope:      Any? = null
    ): Promise<ValidationResult.Outcome>

    companion object {
        val PROTOCOL = typeOf<Validating>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Validating
    }
}

inline fun <reified T: Any> Validating.validate(
        target: T,
        scope:  Any? = null
) = validate(target, typeOf<T>(), scope)

inline fun <reified T: Any> Validating.validateAsync(
        target: T,
        scope:  Any? = null
) = validateAsync(target, typeOf<T>(), scope)

inline fun <reified T: Any> Handling.valid(target: T, scope: Any? = null) =
    aspectBefore({ _, composer ->
        Validating(composer).validate(target, scope).isValid
    })

inline fun <reified T: Any> Handling.validAsync(target: T, scope: Any? = null) =
        aspectBefore({ _, composer ->
            Validating(composer).validateAsync(target, scope) then {
                it.isValid
            }
        })