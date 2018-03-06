package com.miruken.callback

import com.miruken.callback.policy.MethodBinding
import com.miruken.runtime.allInterfaces
import com.miruken.runtime.isAssignableTo
import com.miruken.typeOf
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseFiltersFilterProviderTest {
    @Test fun `Creates closed filter instance`() {
        val x = RequestFilterClosed::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<SuperFoo, Int>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Foo, String>>(), x))
    }

    @Test fun `Creates open filter instance`() {
        val x = RequestFilter::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Int>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<Bar, String>>(), x))
    }

    @Test fun `Creates open bounded filter instance`() {
        val x = RequestFilterBounded::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Int>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Bar, Float>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Foo, String>>(), x))
    }

    @Test fun `Creates open bounded partial filter instance`() {
        val x = RequestFilterBoundedPartial::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<List<Foo>, Map<String,Foo>>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<List<SuperFoo>, Map<String,Foo>>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<List<Bar>, Map<String,Foo>>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<List<Foo>, Map<String,Bar>>>(), x))
    }

    @Test fun `Creates open callback filter instance`() {
        val x = RequestFilterCb::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, String>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<Bar, String>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Bar, Float>>(), x))
    }

    @Test fun `Creates open bounded callback filter instance`() {
        val x = RequestFilterBoundedCb::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Number>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<SuperFoo, Number>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Bar, Number>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Foo, Int>>(), x))
    }

    @Test fun `Creates open result filter instance`() {
        val x = RequestFilterRes::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Number>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<SuperFoo, Any>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Bar, Any>>(), x))
    }

    @Test fun `Creates open bounded result filter instance`() {
        val x = RequestFilterBoundedRes::class.allInterfaces.single {
            it.classifier == Filtering::class
        }
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Number>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<Foo, Int>>(), x))
        assertTrue(isAssignableTo(typeOf<Filtering<SuperFoo, Int>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Bar, Int>>(), x))
        assertFalse(isAssignableTo(typeOf<Filtering<Any, Int>>(), x))
    }

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