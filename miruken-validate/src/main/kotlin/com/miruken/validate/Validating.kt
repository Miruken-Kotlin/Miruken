package com.miruken.validate

import com.miruken.callback.Handling
import com.miruken.callback.aspectBefore
import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Protocol
interface Validating {
    fun validate(
            target:        Any,
            targetType:    KType? = null,
            vararg scopes: KClass<*>
    ): ValidationResult.Outcome

    fun validateAsync(
            target:        Any,
            targetType:    KType? = null,
            vararg scopes: KClass<*>
    ): Promise<ValidationResult.Outcome>

    companion object {
        val PROTOCOL = typeOf<Validating>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Validating
    }
}

inline fun <reified T: Any> Validating.validate(
        target:        T,
        vararg scopes: KClass<*>
) = validate(target, typeOf<T>(), *scopes)

inline fun <reified T: Any> Validating.validateAsync(
        target:        T,
        vararg scopes: KClass<*>
) = validateAsync(target, typeOf<T>(), *scopes)

inline fun <reified T: Any> Handling.valid(
        target:        T,
        vararg scopes: KClass<*>
) = aspectBefore({ _, composer ->
    Validating(composer).validate(target, *scopes).isValid
})

inline fun <reified T: Any> Handling.validAsync(
        target:        T,
        vararg scopes: KClass<*>
) = aspectBefore({ _, composer ->
    Validating(composer).validateAsync(target, *scopes) then {
        it.isValid
    }
})