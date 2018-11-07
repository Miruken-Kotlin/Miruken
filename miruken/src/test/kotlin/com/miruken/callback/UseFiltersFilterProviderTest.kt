package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.kTypeOf
import com.miruken.runtime.isCompatibleWith
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseFiltersFilterProviderTest {
    @Test fun `Creates closed filter instance`() {
        with(RequestFilteringClosed::class) {
            assertTrue  { compatible<Filtering<SpecialFoo, Float>>() }
            assertFalse { compatible<Filtering<Foo, String>>() }
        }
        with(RequestFilterClosed::class) {
            assertTrue  { compatible<Filtering<SpecialFoo, Int>>() }
            assertFalse { compatible<Filtering<Foo, String>>() }
        }
    }

    @Test fun `Creates open filter instance`() {
        with(RequestFiltering::class) {
            assertTrue  { compatible<Filtering<Foo, Int>>() }
            assertTrue  { compatible<Filtering<Bar, String>>() }
            assertTrue  { compatible<Filtering<String, Any>>() }
        }
        with(RequestFilter::class) {
            assertTrue  { compatible<Filtering<Foo, Int>>() }
            assertTrue  { compatible<Filtering<Bar, String>>() }
            assertTrue  { compatible<Filtering<String, Any>>() }
        }
    }

    @Test fun `Creates open bounded filter instance`() {
        with(RequestFilterBounded::class) {
            assertTrue  { compatible<Filtering<Foo, Int>>() }
            assertTrue  { compatible<Filtering<SpecialFoo, Number>>() }
            assertFalse { compatible<Filtering<Bar, Float>>() }
            assertFalse { compatible<Filtering<Foo, String>>() }
        }
    }

    @Test fun `Creates open bounded partial filter instance`() {
        with(RequestFilterBoundedPartial::class) {
            assertTrue  { compatible<Filtering<List<Foo>, Map<String, Foo>>>() }
            assertTrue  { compatible<Filtering<List<SpecialFoo>, Map<String, Foo>>>() }
            assertFalse { compatible<Filtering<List<Bar>, Map<String, Foo>>>() }
            assertFalse { compatible<Filtering<List<Foo>, Map<String, Bar>>>() }
        }
    }

    @Test fun `Creates open callback filter instance`() {
        with(RequestFilterCb::class) {
            assertTrue  { compatible<Filtering<Foo, String>>() }
            assertTrue  { compatible<Filtering<Bar, String>>() }
            assertFalse { compatible<Filtering<Bar, Float>>() }
        }
    }

    @Test fun `Creates open bounded callback filter instance`() {
        with(RequestFilterBoundedCb::class) {
            assertTrue  { compatible<Filtering<Foo, Number>>() }
            assertTrue  { compatible<Filtering<SpecialFoo, Number>>() }
            assertFalse { compatible<Filtering<Bar, Number>>() }
            assertFalse { compatible<Filtering<Foo, Int>>() }
        }
    }

    @Test fun `Creates open result filter instance`() {
        with(RequestFilterRes::class) {
            assertTrue  { compatible<Filtering<Foo, Number>>() }
            assertTrue  { compatible<Filtering<SpecialFoo, Any>>() }
            assertTrue  { compatible<Filtering<Foo, Float>>() }
            assertFalse { compatible<Filtering<Bar, Any>>() }
        }
    }

    @Test fun `Creates open bounded result filter instance`() {
        with(RequestFilterBoundedRes::class) {
            assertTrue  { compatible<Filtering<Foo, Number>>() }
            assertTrue  { compatible<Filtering<Foo, Int>>() }
            assertTrue  { compatible<Filtering<SpecialFoo, Int>>() }
            assertFalse { compatible<Filtering<Bar, Int>>() }
            assertFalse { compatible<Filtering<Any, Int>>() }
        }
    }

    private inline fun <reified T: Filtering<*,*>>
            KClass<out Filtering<*,*>>.compatible(): Boolean {
        val filterType   = kTypeOf<T>()
        val typeBindings = mutableMapOf<KTypeParameter, KType>()
        return getFilteringInterface().let { f ->
            isCompatibleWith(f, filterType, typeBindings) &&
                   typeParameters.all { typeBindings.containsKey(it) }
        }
    }

    open class Foo
    open class SpecialFoo: Foo()
    open class Bar

    interface RequestFilteringClosed: Filtering<SpecialFoo, Float>

    class RequestFilterClosed: Filtering<SpecialFoo, Int>{
        override var order: Int? = null

        override fun next(
                callback: SpecialFoo,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<Int>,
                provider: FilteringProvider?
        ) = next()
    }

    interface RequestFiltering<in T: Any, R: Any?> : Filtering<T, R>

    class RequestFilter<in T: Any, R: Any?> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<R>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterBounded<in T: Foo, R: Number> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<R>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterBoundedPartial<
            out U: Foo, in T: List<U>,
            R: Map<String, U>> : Filtering<T, R> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<R>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterCb<in T: Any> : Filtering<T, String> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<String>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterBoundedCb<in T: Foo> : Filtering<T, Number> {
        override var order: Int? = null

        override fun next(
                callback: T,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<Number>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterRes<T> : Filtering<Foo, T> {
        override var order: Int? = null

        override fun next(
                callback: Foo,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<T>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterBoundedRes<T: Number> : Filtering<Foo, T> {
        override var order: Int? = null

        override fun next(
                callback: Foo,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<T>,
                provider: FilteringProvider?
        ) = next()
    }

    class RequestFilterBad<in T> : Filtering<Any, Any> {
        override var order: Int? = null

        override fun next(
                callback: Any,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<Any>,
                provider: FilteringProvider?
        ) = next()
    }
}