package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.getMethod
import com.miruken.runtime.getKType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.test.*

typealias Foo    = TestHandler.Foo
typealias Bar<T> = TestHandler.Bar<T>

class ArgumentTest {

    @Test fun `Extracts parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handle")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(getKType<Foo>(), argument.parameterType)
        assertEquals(argument.parameterType, argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(getKType<Foo>().withNullability(true), argument.parameterType)
        assertEquals(argument.parameterType.withNullability(false), argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handlePromise")
        val promise  = handle!!.parameters.component2()
        val argument = Argument(promise)
        assertEquals(getKType<Promise<Foo>>(), argument.parameterType)
        assertEquals(getKType<Foo>(), argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleList")
        val list     = handle!!.parameters.component2()
        val argument = Argument(list)
        assertEquals(getKType<List<Foo>>(), argument.parameterType)
        assertEquals(getKType<Foo>(), argument.logicalType)
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleLazy")
        val lazy     = handle!!.parameters.component2()
        val argument = Argument(lazy)
        assertEquals(getKType<Lazy<Foo>>(), argument.parameterType)
        assertEquals(getKType<Foo>(), argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType.isSubtypeOf(getKType<Foo>()) }
        assertEquals(argument.parameterType, argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic optional parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType.withNullability(false)
                .isSubtypeOf(getKType<Foo>()) }
        assertEquals(argument.parameterType.withNullability(false),
                argument.logicalType)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic promise parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType
                .isSubtypeOf(getKType<Promise<Foo>>()) }
        assertTrue  { argument.logicalType.isSubtypeOf(getKType<Foo>()) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic list parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType
                .isSubtypeOf(getKType<Collection<Foo>>()) }
        assertTrue  { argument.logicalType.isSubtypeOf(getKType<Foo>()) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleBoundedGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType
                .isSubtypeOf(getKType<Lazy<Foo>>()) }
        assertTrue  { argument.logicalType.isSubtypeOf(getKType<Foo>()) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType
                .isSubtypeOf(getKType<Any>().withNullability(true)) }
        assertTrue  { argument.logicalType
                .isSubtypeOf(getKType<Any>().withNullability(true)) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic optional parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.parameterType
                .isSubtypeOf(getKType<Any>().withNullability(true)) }
        assertTrue  { argument.logicalType
                .isSubtypeOf(getKType<Any>().withNullability(true)) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic promise parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic list parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic lazy parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open partial generic parameter information`() {
        val handle   = getMethod<TestHandler.OpenGenerics>("handleOpenGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts closed partial generic parameter information`() {
        val handle   = getMethod<TestHandler.Good>("handleClosedGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }
}