package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.getMethod
import kotlin.test.*

class ArgumentTest {
    open class Foo
    open class Bar<T>

    @Suppress("UNUSED_PARAMETER")
    class MyHandler {
        @Handles
        fun handle(cb: Foo) { }

        @Handles
        fun handleOptional(cb: Foo?) {}

        @Handles
        fun handlePromise(cb: Promise<Foo>) {}

        @Handles
        fun handleList(cb: List<Foo>) {}

        @Handles
        fun handleLazy(cb: () -> Foo) {}

        @Handles
        fun <T: Foo> handleBoundedGeneric(cb: T) {}

        @Handles
        fun <T: Foo> handleBoundedGenericOptional(cb: T?) {}

        @Handles
        fun <T: Foo> handleBoundedGenericPromise(cb: Promise<T>) {}

        @Handles
        fun <T: Foo> handleBoundedGenericList(cb: List<T>) {}

        @Handles
        fun <T: Foo> handleBoundedGenericLazy(cb: () -> T) {}

        @Handles
        fun <T> handleOpenGeneric(cb: T) {}

        @Handles
        fun <T> handleOpenGenericOptional(cb: T?) {}

        @Handles
        fun <T> handleOpenGenericPromise(cb: Promise<T>) {}

        @Handles
        fun <T> handleOpenGenericList(cb: List<T>) {}

        @Handles
        fun <T> handleOpenGenericLazy(cb: () -> T) {}

        @Handles
        fun <T> handleOpenGenericPartial(cb: Bar<T>) {}

        @Handles
        fun handleClosedGenericPartial(cb: Bar<String>) {}
    }

    @Test fun `Extracts parameter information`() {
        val handle   = getMethod<MyHandler>("handle")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(argument.parameterType, callback.type)
        assertEquals(Foo::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertEquals(Foo::class, argument.logicalType.classifier)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts optional parameter information`() {
        val handle   = getMethod<MyHandler>("handleOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Foo::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts promise parameter information`() {
        val handle   = getMethod<MyHandler>("handlePromise")
        val promise  = handle!!.parameters.component2()
        val argument = Argument(promise)
        assertEquals(Promise::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts list parameter information`() {
        val handle   = getMethod<MyHandler>("handleList")
        val list     = handle!!.parameters.component2()
        val argument = Argument(list)
        assertEquals(List::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts lazy parameter information`() {
        val handle   = getMethod<MyHandler>("handleLazy")
        val lazy     = handle!!.parameters.component2()
        val argument = Argument(lazy)
        assertEquals(Function0::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic parameter information`() {
        val handle   = getMethod<MyHandler>("handleBoundedGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Foo::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic optional parameter information`() {
        val handle   = getMethod<MyHandler>("handleBoundedGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Foo::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic promise parameter information`() {
        val handle   = getMethod<MyHandler>("handleBoundedGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Promise::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic list parameter information`() {
        val handle   = getMethod<MyHandler>("handleBoundedGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(List::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts bounded generic lazy parameter information`() {
        val handle   = getMethod<MyHandler>("handleBoundedGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Function0::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGeneric")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Any::class, argument.parameterClass)
        assertEquals(Any::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic optional parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGenericOptional")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Any::class, argument.parameterClass)
        assertEquals(Any::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic promise parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGenericPromise")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Promise::class, argument.parameterClass)
        assertEquals(Any::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic list parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGenericList")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(List::class, argument.parameterClass)
        assertEquals(Any::class, argument.logicalClass)
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open generic lazy parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGenericLazy")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Function0::class, argument.parameterClass)
        assertEquals(Any::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts open partial generic parameter information`() {
        val handle   = getMethod<MyHandler>("handleOpenGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Bar::class, argument.parameterClass)
        assertEquals(Bar::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertTrue  { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }

    @Test fun `Extracts closed partial generic parameter information`() {
        val handle   = getMethod<MyHandler>("handleClosedGenericPartial")
        val callback = handle!!.parameters.component2()
        val argument = Argument(callback)
        assertEquals(Bar::class, argument.parameterClass)
        assertEquals(Bar::class, argument.logicalClass)
        assertFalse { argument.flags.hasFlag(ArgumentFlags.COLLECTION) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.LAZY) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPTIONAL) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.PROMISE) }
        assertFalse { argument.flags.hasFlag(ArgumentFlags.OPEN) }
    }
}