package com.miruken.callback

import com.miruken.callback.policy.HandleMethodBinding
import com.miruken.runtime.isGeneric
import com.miruken.typeOf

inline operator fun <reified T: Handling,
        reified S: Any> T.plus(other: S): Handling =
        CascadeHandler(this.toHandler(), other.toHandler())

inline fun <reified T: Any> Handling.provide(value: T) =
        CascadeHandler(Provider(value, typeOf<T>()), this)

inline fun <reified T: Any> T.toHandler(): Handling {
    return if (this::class.isGeneric)
        GenericWrapper(this, typeOf<T>())
    else
        this as? Handling ?: HandlerAdapter(this)
}

inline val COMPOSER get() = HandleMethodBinding.COMPOSER.get()

fun requireComposer() = COMPOSER ?: error(
        "Composer is not available.  Did you call this method directly?")

fun notHandled(): Nothing =
        throw HandleResultException(HandleResult.NOT_HANDLED)

fun notHandledAndStop(): Nothing =
        throw HandleResultException(HandleResult.NOT_HANDLED_AND_STOP)
