package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

class UseFiltersFilterProviderTest {
    private lateinit var _parameters:
            MutableMap<KTypeParameter, KType>

    @Before
    fun setup() {
        _parameters = mutableMapOf()
    }

    @Test fun `Creates closed filter instance`() {
        with(RequestFilterClosed::class) {
            assertCompatible<Filtering<SuperFoo, Int>>(this)
            assertNotCompatible<Filtering<Foo, String>>(this)
        }
    }

    @Test fun `Creates open filter instance`() {
        with(RequestFilter::class) {
            assertCompatible<Filtering<Foo, Int>>(this)
            assertCompatible<Filtering<Bar, String>>(this)
            assertCompatible<Filtering<String, Any>>(this)
        }
    }

    @Test fun `Creates open bounded filter instance`() {
        with(RequestFilterBounded::class) {
            assertCompatible<Filtering<Foo, Int>>(this)
            assertCompatible<Filtering<SuperFoo, Number>>(this)
            assertNotCompatible<Filtering<Bar, Float>>(this)
            assertNotCompatible<Filtering<Foo, String>>(this)
        }
    }

    @Test fun `Creates open bounded partial filter instance`() {
        with(RequestFilterBoundedPartial::class) {
            assertCompatible<Filtering<List<Foo>, Map<String, Foo>>>(this)
            assertCompatible<Filtering<List<SuperFoo>, Map<String, Foo>>>(this)
            assertNotCompatible<Filtering<List<Bar>, Map<String, Foo>>>(this)
            assertNotCompatible<Filtering<List<Foo>, Map<String, Bar>>>(this)
        }
    }

    @Test fun `Creates open callback filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, String>>(this)
            assertCompatible<Filtering<Bar, String>>(this)
            assertNotCompatible<Filtering<Bar, Float>>(this)
        }
    }

    @Test fun `Creates open bounded callback filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>(this)
            assertCompatible<Filtering<SuperFoo, Number>>(this)
            assertNotCompatible<Filtering<Bar, Number>>(this)
            assertNotCompatible<Filtering<Foo, Int>>(this)
        }
    }

    @Test fun `Creates open result filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>(this)
            assertCompatible<Filtering<SuperFoo, Any>>(this)
            assertNotCompatible<Filtering<Bar, Any>>(this)
        }
    }

    @Test fun `Creates open bounded result filter instance`() {
        with(RequestFilterCb::class) {
            assertCompatible<Filtering<Foo, Number>>(this)
            assertCompatible<Filtering<Foo, Int>>(this)
            assertCompatible<Filtering<SuperFoo, Int>>(this)
            assertNotCompatible<Filtering<Bar, Int>>(this)
            assertNotCompatible<Filtering<Any, Int>>(this)
        }
    }

    private inline fun <reified T: Filtering<*,*>> assertCompatible(
            fc: KClass<*>
    ): Boolean {
        _parameters.clear()
        val filterType = typeOf<T>()
        return getFilterInterface(fc)?.let {
            isCompatibleWith(filterType, it, _parameters) &&
                    filterType.arguments.zip(fc.typeParameters) {
                        typeProjection, typeParameter ->
                        _parameters[typeParameter] == typeProjection.type
                    }.all { it }
        } ?: false
    }

    private inline fun <reified T: Filtering<*,*>> assertNotCompatible(
            fc: KClass<*>
    ) = !isCompatibleWith(typeOf<T>(), fc)

    open class Foo
    open class SuperFoo: Foo()
    open class Bar

    class RequestFilterClosed: Filtering<SuperFoo, Int>{
        override var order: Int? = null

        override fun next(
                callback: SuperFoo,
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