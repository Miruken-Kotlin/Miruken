package com.miruken.validate

import com.miruken.callback.Handling
import com.miruken.callback.aspectBefore
import com.miruken.concurrent.Promise
import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface Validating {
    fun validate(
            target:        Any,
            vararg scopes: Any
    ): ValidationResult.Outcome

    fun validateAsync(
            target:        Any,
            vararg scopes: Any
    ): Promise<ValidationResult.Outcome>

    companion object {
        val PROTOCOL = typeOf<Validating>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Validating
    }
}

fun Handling.valid(target: Any, vararg scopes: Any) =
    aspectBefore({ _, composer ->
        Validating(composer).validate(target, scopes).isValid
    })

fun Handling.validAsync(target: Any, vararg scopes: Any) =
        aspectBefore({ _, composer ->
            Validating(composer).validateAsync(target, scopes) then {
                it.isValid
            }
        })