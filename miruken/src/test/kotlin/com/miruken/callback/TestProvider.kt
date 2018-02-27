package com.miruken.callback

import com.miruken.concurrent.Promise

@Suppress("UNUSED_PARAMETER")
sealed class TestProvider {
    open class Foo
    open class Bar<T>
    open class Baz<T, R>

    class Good : TestProvider() {
        @Provides
        fun provideAny(): Any = Foo()

        @Provides
        fun provideOptionalAny(): Any? = Bar<Int>()

        @Provides
        fun provide(): Foo = Foo()

        @Provides
        fun provideCallback(cb: Inquiry) {}

        @Provides
        fun provideCallbackReturn(cb: Inquiry) = Foo()

        @Provides
        fun provideComposer(composer: Handling): Foo = Foo()

        @Provides
        fun provideOptional(): Foo? = null

        @Provides
        fun providePromise(): Promise<Foo> = Promise.resolve(Foo())

        @Provides
        fun provideList(): List<Foo> = listOf(Foo())

        @Provides
        fun provideLazy(): Lazy<Foo> = lazy { Foo() }

        @Provides
        fun provideFunc(): () -> Foo = { Foo() }

        @Provides
        fun provideLazy3(): () -> Foo = fun(): Foo { return Foo() }

        @Provides
        fun <T : Foo> provideBoundedGeneric(): Bar<T> = Bar()

        @Provides
        fun <T : Foo> provideBoundedGenericOptional(): Bar<T>? = null

        @Provides
        fun <T : Foo> provideBoundedGenericPromise(): Promise<Bar<T>> =
                Promise.resolve(Bar())

        @Provides
        fun <T : Foo> provideBoundedGenericList(): Collection<Bar<T>> =
                listOf(Bar())

        @Provides
        fun <T : Foo, S> provideBoundedPartialGeneric(): Baz<T, S> = Baz()
    }

    class Properties : TestProvider() {
        @Provides
        val provide = Foo()

        @Provides
        val provideGetter: Foo
            get() = Foo()
    }

    class GenericProperties<T: Foo> : TestProvider() {
        @Provides
        val provide = Foo()

        @Provides
        val provideGetter: Foo
            get() = Foo()
    }

    class ReturnsNothing : TestProvider() {
        @Provides
        fun provide(): Nothing = throw Exception()
    }

    class ReturnsNothingWithCallback : TestProvider() {
        @Provides
        fun provide(cb: Inquiry): Nothing = throw Exception()
    }

    class ReturnsUnit : TestProvider() {
        @Provides
        fun provideNothing() = Unit
    }
}
