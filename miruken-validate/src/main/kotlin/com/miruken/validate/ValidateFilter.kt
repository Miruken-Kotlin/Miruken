package com.miruken.validate

import com.miruken.Stage
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.concurrent.flatMap

class ValidateFilter<in Cb: Any, Res: Any?>
    @Provides @Singleton constructor() : Filtering<Cb, Res> {

    override var order: Int? = Stage.VALIDATION

    override fun next(
            callback:    Cb,
            rawCallback: Any,
            binding:     MemberBinding,
            composer:    Handling,
            next:        Next<Res>,
            provider:    FilteringProvider?
    ) = validate(callback, composer) flatMap {
        next()
    } flatMap {
        @Suppress("UNCHECKED_CAST")
        if (it == null) {
            Promise.EMPTY as Promise<Res>
        } else {
            validate(it, composer) as Promise<Res>
        }
    }

    private fun validate(target: Any, handler: Handling) =
            handler.validateAsync(target) then {
                if (!it.isValid)
                    throw ValidationException(it)
                target
            }
}