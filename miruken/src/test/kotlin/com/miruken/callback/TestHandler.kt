package com.miruken.callback

import com.miruken.concurrent.Promise

@Suppress("UNUSED_PARAMETER")
sealed class TestHandler {
    open class Foo
    open class Bar<T>
    open class Baz<T, R>

    class Good : TestHandler() {
        @Handles
        fun handleAny(cb: Any) {
        }

        @Handles
        fun handleOptionalAny(cb: Any?) {
        }

        @Handles
        fun handle(cb: Foo) {
        }

        @Handles
        fun handleResult(cb: Foo) = HandleResult.HANDLED

        @Handles
        fun handleComposer(cb: Foo, composer: Handling) {
        }

        @Handles
        fun handleOptional(cb: Foo?) {
        }

        @Handles
        fun handlePromise(cb: Promise<Foo>) {
        }

        @Handles
        fun handleList(cb: List<Foo>) {
        }

        @Handles
        fun handleLazy(cb: Lazy<Foo>) {
        }

        @Handles
        fun handleLazy2(cb: () -> Foo) {
        }

        @Handles
        fun handleCallback(cb: Command) {
        }

        @Handles
        fun handleTarget(target: Any, cb: Command) {
        }

        @Handles
        fun handleTargetComposer(target: Any, cb: Command, composer: Handling) {
        }

        @Handles
        fun <T : Foo> handleBoundedGeneric(cb: T) {
        }

        @Handles
        fun <T : Foo> handleBoundedGenericOptional(cb: T?) {
        }

        @Handles
        fun <T : Foo> handleBoundedGenericPromise(cb: Promise<T>) {
        }

        @Handles
        fun <T : Foo> handleBoundedGenericList(cb: Collection<T>) {
        }

        @Handles
        fun <T : Foo> handleBoundedGenericLazy(cb: Lazy<T>) {
        }

        @Handles
        fun <T : Foo> handleBoundedGenericLazy2(cb: () -> T) {
        }

        @Handles
        fun <T : Foo, S> handleBoundedPartialGeneric(cb: Baz<T, S>) {
        }

        @Handles
        fun handleClosedGenericPartial(cb: Bar<String>) {
        }
    }

    class NoParameters : TestHandler() {
        @Handles
        fun handle() {}
    }

    class NothingParameter : TestHandler() {
        @Handles
        fun handle(cb: Nothing) {}
    }

    class OpenGenerics : TestHandler() {
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
        fun <T> handleOpenGenericPartial(cb: TestHandler.Bar<T>) {}
    }
}


