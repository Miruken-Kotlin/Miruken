package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.callback.TestHandler
import com.miruken.concurrent.Promise
import com.miruken.getMethod
import com.miruken.runtime.ANY_TYPE
import com.miruken.runtime.isCompatibleWith
import com.miruken.typeOf
import kotlin.reflect.KTypeParameter
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
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Foo>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.PRIMITIVE))
        assertFalse(flags.has(TypeFlags.INTERFACE))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts primitive parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handlePrimitive")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Int>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.PRIMITIVE))
        assertFalse(flags.has(TypeFlags.INTERFACE))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts interface parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleInterface")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Boo>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.INTERFACE))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Foo>().withNullability(true), argument.parameterType)
        assertEquals(argument.parameterType.withNullability(false), argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertTrue(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handlePromise")
        val promise  = handle!!.parameters.component2()
        val argument = Argument(promise)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Promise<Foo>>(), argument.parameterType)
        assertEquals(typeOf<Foo>(), argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertTrue(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleList")
        val list     = handle!!.parameters.component2()
        val argument = Argument(list)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<List<Foo>>(), argument.parameterType)
        assertEquals(typeOf<List<Foo>>(), argument.typeInfo.logicalType)
        assertEquals(typeOf<Foo>(), argument.typeInfo.componentType)
        assertTrue(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleLazy")
        val lazy     = handle!!.parameters.component2()
        val argument = Argument(lazy)
        val flags    = argument.typeInfo.flags
        assertEquals(typeOf<Lazy<Foo>>(), argument.parameterType)
        assertEquals(typeOf<Foo>(), argument.typeInfo.logicalType)
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertTrue(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.parameterType))
        val classifier = argument.typeInfo.logicalType.classifier as? KTypeParameter
        assertNotNull(classifier!!)
        assertTrue(classifier.upperBounds.contains(typeOf<Foo>()))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.parameterType))
        val classifier = argument.typeInfo.logicalType.classifier as? KTypeParameter
        assertNotNull(classifier!!)
        assertTrue(classifier.upperBounds.contains(typeOf<Foo>()))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertTrue(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Promise<Foo>>(), argument.parameterType))
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.typeInfo.logicalType))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertTrue(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Collection<Foo>>(), argument.parameterType))
        assertTrue(isCompatibleWith(typeOf<Collection<Foo>>(), argument.typeInfo.logicalType))
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.typeInfo.componentType))
        assertTrue(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Lazy<Foo>>(), argument.parameterType))
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.typeInfo.logicalType))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertTrue(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts bounded generic lazy func parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericFunc")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Function0<Foo>>(), argument.parameterType))
        assertTrue(isCompatibleWith(typeOf<Foo>(), argument.typeInfo.logicalType))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertTrue(flags.has(TypeFlags.FUNC))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(typeOf<Any>(), argument.parameterType))
        assertTrue(isCompatibleWith(typeOf<Any>(), argument.typeInfo.logicalType))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic optional parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(isCompatibleWith(ANY_TYPE, argument.parameterType))
        assertTrue(isCompatibleWith(ANY_TYPE, argument.typeInfo.logicalType))
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertTrue(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic promise parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertTrue(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic list parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertTrue(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertTrue(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts open partial generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertTrue(flags.has(TypeFlags.OPEN))
    }

    @Test fun `Extracts closed partial generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleClosedGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        val flags    = argument.typeInfo.flags
        assertFalse(flags.has(TypeFlags.COLLECTION))
        assertFalse(flags.has(TypeFlags.LAZY))
        assertFalse(flags.has(TypeFlags.PROMISE))
        assertFalse(flags.has(TypeFlags.OPTIONAL))
        assertFalse(flags.has(TypeFlags.OPEN))
    }
}