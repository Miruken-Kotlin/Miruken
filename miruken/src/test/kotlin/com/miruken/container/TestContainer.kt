package com.miruken.container

import com.miruken.callback.Handler
import com.miruken.callback.notHandled
import com.miruken.concurrent.Promise
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance

class TestContainer : Handler(), Container {
    override fun resolve(key: Any) =
            ((key as? KType)?.classifier as? KClass<*>)?.run {
                createInstance()
            } ?: notHandled()

    override fun resolveAsync(key: Any) =
        ((key as? KType)?.classifier as? KClass<*>)?.run {
            Promise.resolve(createInstance())
        } ?: notHandled()

    override fun resolveAll(key: Any) = emptyList<Any>()

    override fun resolveAllAsync(key: Any) = Promise.EMPTY_LIST

    override fun release(component: Any) {
    }
}