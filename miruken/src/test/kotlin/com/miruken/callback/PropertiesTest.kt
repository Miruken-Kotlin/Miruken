package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.concurrent.Promise
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.*

class PropertiesTest {
    @Rule
    @JvmField val testName = TestName()

    class Foo

    @Test fun `Delegates property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo: Foo by handler.get()
        }
        assertSame(foo, instance.foo)
    }

    @Test fun `Delegates optional property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo: Foo? by handler.get()
        }
        assertSame(foo, instance.foo)
    }

    @Test fun `Ignores missing optional property`() {
        val handler  = Handler()
        val instance = object {
            val foo: Foo? by handler.get()
        }
        assertNull(instance.foo)
    }

    @Test fun `Delegates list property to handler`() {
        val handler  = object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        }
        val instance = object {
            val foos: List<Foo> by handler.get()
        }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Delegates array property to handler`() {
        val handler  = object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        }
        val instance = object {
            val foos: Array<Foo> by handler.get()
        }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Uses empty list property if missing`() {
        val instance = object {
            val foos: List<Foo> by Handler().get()
        }
        assertEquals(0, instance.foos.size)
    }

    @Test fun `Delegates promise property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo: Promise<Foo> by handler.get()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertSame(foo, it)
                done()
            }
        }
    }

    @Test fun `Delegates optional promise property to handler`() {
        val handler  = Handler()
        val instance = object {
            val foo: Promise<Foo?> by handler.get()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertNull(it)
                done()
            }
        }
    }

    @Test fun `Delegates promise list property to handler`() {
        val handler  = object : Handler() {
            @Provides
            fun provideFoos() = Promise.resolve(listOf(Foo(), Foo()))
        }
        val instance = object {
            val foo: Promise<List<Foo>> by handler.get()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Delegates promise array property to handler`() {
        val handler  = object : Handler() {
            @Provides
            fun provideFoos() = Promise.resolve(listOf(Foo(), Foo()))
        }
        val instance = object {
            val foo: Promise<Array<Foo>> by handler.get()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Rejects property delegation if missing`() {
        val handler  = Handler()
        val instance = object {
            val foo: Foo by handler.get()
        }
        assertFailsWith(IllegalStateException::class) {
            instance.foo
        }
    }

    @Test fun `Rejects promise property delegation if missing`() {
        val handler  = Handler()
        val instance = object {
            val foo: Promise<Foo> by handler.get()
        }
        assertAsync(testName) { done ->
            instance.foo catch {
                assertTrue(it is IllegalStateException)
                done()
            }
        }
    }
}