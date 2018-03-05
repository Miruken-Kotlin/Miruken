package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.getMethod
import com.miruken.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.test.*

typealias Boo    = TestHandler.Boo
typealias Foo    = TestHandler.Foo
typealias Bar<T> = TestHandler.Bar<T>

class ArgumentTest {

    @Test fun `Extracts parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handle")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(typeOf<Foo>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.PRIMITIVE))
        assertFalse(argument.flags.has(TypeFlags.INTERFACE))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts primitive parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handlePrimitive")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(typeOf<Int>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.PRIMITIVE))
        assertFalse(argument.flags.has(TypeFlags.INTERFACE))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts interface parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleInterface")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(typeOf<Boo>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.INTERFACE))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(typeOf<Foo>().withNullability(true), argument.parameterType)
        assertEquals(argument.parameterType.withNullability(false), argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertTrue(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handlePromise")
        val promise  = handle!!.parameters.component2()
        val argument = Argument(promise)
        assertEquals(typeOf<Promise<Foo>>(), argument.parameterType)
        assertEquals(typeOf<Foo>(), argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertTrue(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleList")
        val list     = handle!!.parameters.component2()
        val argument = Argument(list)
        assertEquals(typeOf<List<Foo>>(), argument.parameterType)
        assertEquals(typeOf<Foo>(), argument.logicalType)
        assertTrue(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleLazy")
        val lazy     = handle!!.parameters.component2()
        val argument = Argument(lazy)
        assertEquals(typeOf<Lazy<Foo>>(), argument.parameterType)
        assertEquals(typeOf<Foo>(), argument.logicalType)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertTrue(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType.isSubtypeOf(typeOf<Foo>()))
        val classifier = argument.logicalType.classifier as? KTypeParameter
        assertNotNull(classifier!!)
        assertTrue(classifier.upperBounds.contains(typeOf<Foo>()))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType.withNullability(false)
                .isSubtypeOf(typeOf<Foo>()))
        val classifier = argument.logicalType.classifier as? KTypeParameter
        assertNotNull(classifier!!)
        assertTrue(classifier.upperBounds.contains(typeOf<Foo>()))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertTrue(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Promise<Foo>>()))
        assertTrue(argument.logicalType.isSubtypeOf(typeOf<Foo>()))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertTrue(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Collection<Foo>>()))
        assertTrue(argument.logicalType.isSubtypeOf(typeOf<Foo>()))
        assertTrue(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Lazy<Foo>>()))
        assertTrue(argument.logicalType.isSubtypeOf(typeOf<Foo>()))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertTrue(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic lazy func parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericFunc")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Function0<Foo>>()))
        assertTrue(argument.logicalType.isSubtypeOf(typeOf<Foo>()))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertTrue(argument.flags.has(TypeFlags.FUNC))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Any>().withNullability(true)))
        assertTrue(argument.logicalType
                .isSubtypeOf(typeOf<Any>().withNullability(true)))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic optional parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.parameterType
                .isSubtypeOf(typeOf<Any>().withNullability(true)))
        assertTrue(argument.logicalType
                .isSubtypeOf(typeOf<Any>().withNullability(true)))
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertTrue(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic promise parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertTrue(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic list parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertTrue(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open partial generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertTrue(argument.flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts closed partial generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleClosedGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse(argument.flags.has(TypeFlags.COLLECTION))
        assertFalse(argument.flags.has(TypeFlags.LAZY))
        assertFalse(argument.flags.has(TypeFlags.PROMISE))
        assertFalse(argument.flags.has(TypeFlags.OPTIONAL))
        assertFalse(argument.flags.has(TypeFlags.OPEN))
    }
}