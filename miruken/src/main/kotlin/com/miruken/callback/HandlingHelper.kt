package com.miruken.callback

import com.miruken.runtime.isGeneric
import com.miruken.runtime.typeOf

inline operator fun <reified T: Handling,
        reified S: Any> T.plus(other: S): Handling =
        CascadeHandler(this.toHandler(), other.toHandler())

inline fun <reified T: Any> Handling.provide(value: T) =
        CascadeHandler(Provider(value, typeOf<T>()), this)

inline fun <reified T: Any> T.toHandler() : Handling {
    val handler = this as? Handling ?: HandlerAdapter(this)
    return if (this::class.isGeneric)
        CascadeHandler(Provider(this, typeOf<T>()), handler)
    else handler
}