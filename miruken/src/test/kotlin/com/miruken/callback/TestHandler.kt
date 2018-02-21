package com.miruken.callback

import com.miruken.concurrent.Promise

@Suppress("UNUSED_PARAMETER")
class TestHandler {
    open class Foo
    open class Bar<T>

    @Handles
    fun handleNothing() {}

    @Handles
    fun handle(cb: Foo) {}

    @Handles
    fun handleOptional(cb: Foo?) {}

    @Handles
    fun handlePromise(cb: Promise<Foo>) {}

    @Handles
    fun handleList(cb: List<Foo>) {}

    @Handles
    fun handleLazy(cb: Lazy<Foo>) {}

    @Handles
    fun handleLazy2(cb: () -> Foo) {}

    @Handles
    fun <T: Foo> handleBoundedGeneric(cb: T) {}

    @Handles
    fun <T: Foo> handleBoundedGenericOptional(cb: T?) {}

    @Handles
    fun <T: Foo> handleBoundedGenericPromise(cb: Promise<T>) {}

    @Handles
    fun <T: Foo> handleBoundedGenericList(cb: Collection<T>) {}

    @Handles
    fun <T: Foo> handleBoundedGenericLazy(cb: Lazy<T>) {}

    @Handles
    fun <T: Foo> handleBoundedGenericLazy2(cb: () -> T) {}

    @Handles
    fun <T> handleOpenGeneric(cb: T) {}

    @Handles
    fun <T> handleOpenGenericOptional(cb: T?) {}

    @Handles
    fun <T> handleOpenGenericPromise(cb: Promise<T>) {}

    @Handles
    fun <T> handleOpenGenericList(cb: Collection<T>) {}

    @Handles
    fun <T> handleOpenGenericLazy(cb: Lazy<T>) {}

    @Handles
    fun <T> handleOpenGenericLazy2(cb: () -> T) {}

    @Handles
    fun <T> handleOpenGenericPartial(cb: Bar<T>) {}

    @Handles
    fun handleClosedGenericPartial(cb: Bar<String>) {}
}