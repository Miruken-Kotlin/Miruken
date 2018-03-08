package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

class UseFiltersFilterProviderTest {
    @Test fun `Creates closed filter instance`() {
        with(RequestFilterClosed::class) {
            assertCompatible<Filtering<SpecialFoo, Int>>()
            assertNotCompatible<Filtering<Foo, String>>()
        }
    }

    @Test fun `Creates open filter instance`() {
        with(RequestFilter::class) {
            assertCompatible<Filtering<Foo, Int>>()
            assertCompatible<Filtering<Bar, String>>()
            assertCompatible<Filtering<String, Any>>()
        }
    }

    @Test fun `Creates open bounded filter instance`() {
        with(RequestFilterBounded::class) {
            assertCompatible<Filtering<Foo, Int>>()
            assertCompatible<Filtering<SpecialFoo, Number>>()
            assertNotCompatible<Filtering<Bar, Float>>()
            assertNotCompatible<Filtering<Foo, String>>()
        }
    }

    @Test fun `Creates open bounded partial filter instance`() {
        with(RequestFilterBoundedPartial::class) {
            assertCompatible<Filtering<List<Foo>, Map<String, Foo>>>()
            assertCompatible<Filtering<List<SpecialFoo>, Map<String, Foo>>>()
            assertNotCompatible<Filtering<List<Bar>, Map<String, Foo>>>()
            assertNotCompatible<Filtering<List<Foo>, Map<String, Bar>>>()
        }
    }

    @Test fun `Creates open callback filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, String>>()
            assertCompatible<Filtering<Bar, String>>()
            assertNotCompatible<Filtering<Bar, Float>>()
        }
    }

    @Test fun `Creates open bounded callback filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>()
            assertCompatible<Filtering<SpecialFoo, Number>>()
            assertNotCompatible<Filtering<Bar, Number>>()
            assertNotCompatible<Filtering<Foo, Int>>()
        }
    }

    @Test fun `Creates open result filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>()
            assertCompatible<Filtering<SpecialFoo, Any>>()
            assertNotCompatible<Filtering<Bar, Any>>()
        }
    }

    @Test fun `Creates open bounded result filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>()
            assertCompatible<Filtering<Foo, Int>>()
            assertCompatible<Filtering<SpecialFoo, Int>>()
            assertNotCompatible<Filtering<Bar, Int>>()
            assertNotCompatible<Filtering<Any, Int>>()
        }
    }

    private inline fun <reified T: Filtering<*,*>>
            KClass<out Filtering<*,*>>.assertCompatible(): Boolean {
        val filterType   = typeOf<T>()
        val typeBindings = mutableMapOf<KTypeParameter, KType>()
        return getFilteringInterface().let {
            isCompatibleWith(filterType, it, typeBindings) &&
                    filterType.arguments.zip(this.typeParameters) {
                        typeProjection, typeParameter ->
                        typeBindings[typeParameter] == typeProjection.type
                    }.all { it }
        }
    }

    private inline fun <reified T: Filtering<*,*>>
            KClass<*>.assertNotCompatible() =
            !isCompatibleWith(typeOf<T>(), this)

    open class Foo
    open class SpecialFoo: Foo()
    open class Bar

    class RequestFilterClosed: Filtering<SpecialFoo, Int>{
        override var order: Int? = null

        override fun next(
                callback: SpecialFoo,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Int>
        ) = next(composer)
    }

    class RequestFilter<in T, R> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<R>
        ) = next(composer)
    }

    class RequestFilterBounded<in T: Foo, R: Number> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<R>
        ) = next(composer)
    }

    class RequestFilterBoundedPartial<
            out U: Foo, in T: List<U>,
            R: Map<String, U>> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<R>
        ) = next(composer)
    }

    class RequestFilterCb<in T> : Filtering<T, String> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<String>
        ) = next(composer)
    }

    class RequestFilterBoundedCb<in T: Foo> : Filtering<T, Number> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Number>
        ) = next(composer)
    }

    class RequestFilterRes<T> : Filtering<Foo, T> {
        override var order: Int? = null

        override fun next(
                callback: Foo,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<T>
        ) = next(composer)
    }

    class RequestFilterBoundedRes<T: Number> : Filtering<Foo, T> {
        override var order: Int? = null

        override fun next(
                callback: Foo,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<T>
        ) = next(composer)
    }

    class RequestFilterBad<in T> : Filtering<Any, Any> {
        override var order: Int? = null

        override fun next(
                callback: Any,
                binding:  MethodBinding,
                composer: Handling,
                next:     Next<Any>
        ) = next(composer)
    }
}