package com.miruken.callback.policy

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.getMethod
import kotlin.test.*
import org.junit.Test as test

class ArgumentTest {
    class Foo

    class MyHandler {
        @Handles
        fun handle(cb: Foo) {}
        fun handleOptional(cb: Foo?) {}
        fun handlePromise(cb: Promise<Foo>) {}
        fun handleList(cb: List<Foo>) {}
        fun handleLazy(cb: () -> Foo) {}
        fun <T: Foo> handleGeneric(cb: T) {}
    }

    @test fun `Extracts parameter information`() {
        val handle   = getMethod<MyHandler>("handle")
        val callback = handle.parameters.component2()
        val argument = Argument(callback)
        assertSame(callback, argument.parameter)
        assertEquals(Foo::class, argument.parameterClass)
        assertFalse { argument.isList }
        assertFalse { argument.isLazy }
        assertFalse { argument.isOptional }
        assertFalse { argument.isPromise }
    }

    @test fun `Extracts optional parameter information`() {
        val handle   = getMethod<MyHandler>("handleOptional")
        val callback = handle.parameters.component2()
        val argument = Argument(callback)
        assertSame(callback, argument.parameter)
        assertEquals(Foo::class, argument.parameterClass)
        assertFalse { argument.isList }
        assertFalse { argument.isLazy }
        assertTrue  { argument.isOptional }
        assertFalse { argument.isPromise }
    }

    @test fun `Extracts Promise parameter information`() {
        val handle   = getMethod<MyHandler>("handlePromise")
        val promise  = handle.parameters.component2()
        val argument = Argument(promise)
        assertEquals(Promise::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.isList }
        assertFalse { argument.isLazy }
        assertFalse { argument.isOptional }
        assertTrue  { argument.isPromise }
    }

    @test fun `Extracts list parameter information`() {
        val handle   = getMethod<MyHandler>("handleList")
        val list     = handle.parameters.component2()
        val argument = Argument(list)
        assertEquals(List::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertTrue  { argument.isList }
        assertFalse { argument.isLazy }
        assertFalse { argument.isOptional }
        assertFalse { argument.isPromise }
    }

    @test fun `Extracts lazy parameter information`() {
        val handle   = getMethod<MyHandler>("handleLazy")
        val lazy     = handle.parameters.component2()
        val argument = Argument(lazy)
        assertEquals(Function0::class, argument.parameterClass)
        assertEquals(Foo::class, argument.logicalClass)
        assertFalse { argument.isList }
        assertTrue  { argument.isLazy }
        assertFalse { argument.isOptional }
        assertFalse { argument.isPromise }
    }

    @Ignore
    @test fun `Extracts generic parameter information`() {
        val handle   = getMethod<MyHandler>("handleGeneric")
        val callback = handle.parameters.component2()
        val argument = Argument(callback)
    }
}