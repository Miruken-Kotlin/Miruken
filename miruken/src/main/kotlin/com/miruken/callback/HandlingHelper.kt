package com.miruken.callback

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.TypeReference
import com.miruken.runtime.isGeneric
import com.miruken.typeOf

inline operator fun <reified T: Handling,
        reified S: Any> T.plus(other: S): Handling =
        CascadeHandler(this.toHandler(), other.toHandler())

inline fun <reified T: Any> Handling.provide(value: T) =
        CascadeHandler(Provider(value, typeOf<T>()), this)

inline fun <reified T: Any> Handling.with(value: T) =
        CascadeHandler(Provider(value, typeOf<T>()), this)

inline fun <reified T: Any> T.toHandler(): Handling {
    return if (this::class.isGeneric)
        GenericWrapper(this, typeOf<T>())
    else
        this as? Handling ?: HandlerAdapter(this)
}

fun Handling.resolveArgs(vararg types: TypeReference): List<Any?>? {
    return types.map { key ->
        val type = key.kotlinType
        val typeInfo = TypeInfo.parse(type)
        val inquiry = typeInfo.createInquiry(type)
        KeyResolver.resolve(inquiry, typeInfo, this)
                ?: if (typeInfo.flags has TypeFlags.OPTIONAL)
                    null else return null
    }
}

val COMPOSER get() = threadComposer.get()

fun requireComposer() = COMPOSER ?: error(
        "Composer is not available.  Did you call this method directly?")

fun notHandled(): Nothing =
        throw HandleResultException(HandleResult.NOT_HANDLED)

fun notHandledAndStop(): Nothing =
        throw HandleResultException(HandleResult.NOT_HANDLED_AND_STOP)

fun <R> withComposer(composer: Handling, block: () -> R): R {
    val oldComposer = threadComposer.get()
    if (oldComposer === composer) return block()
    return try {
        threadComposer.set(composer)
        block()
    } finally {
        threadComposer.set(oldComposer)
    }
}

private val threadComposer = ThreadLocal<Handling?>()

