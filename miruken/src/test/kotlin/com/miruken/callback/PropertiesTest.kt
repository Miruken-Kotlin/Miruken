package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.concurrent.Promise
import com.miruken.container.Managed
import com.miruken.container.TestContainer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*
import kotlin.test.*

class PropertiesTest {
    @Rule
    @JvmField val testName = TestName()

    class Foo
    interface Auction {
        fun buy(itemId: Long): UUID
    }

    @Test fun `Delegates property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo by handler.get<Foo>()
        }
        assertSame(foo, instance.foo)
    }

    @Test fun `Delegates optional property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo by handler.get<Foo?>()
        }
        assertSame(foo, instance.foo)
    }

    @Test fun `Ignores missing optional property`() {
        val handler  = Handler()
        val instance = object {
            val foo by handler.get<Foo?>()
        }
        assertNull(instance.foo)
    }

    @Test fun `Delegates list property to handler`() {
        val handler  = object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        }
        val instance = object {
            val foos by handler.get<List<Foo>>()
        }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Delegates array property to handler`() {
        val handler = object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        }
        val instance = object {
            val foos by handler.get<Array<Foo>>()
        }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Delegates primitive property to handler`() {
        val handler = object : Handler() {
            @Provides
            val primes = listOf(2,3,5,7,11)

            @Provides
            @Key("help")
            val primaryHelp = "www.help.com"

            @Provides
            @Key("help")
            val secondaryHelp = "www.help2.com"

            @Provides
            @Key("help")
            val criticalHelp = "www.help3.com"
        }
        val instance = object {
            val primes by handler.get<IntArray>()
            val help by handler.get<Array<String>>()
        }
        assertTrue(instance.primes.contentEquals(arrayOf(2,3,5,7,11).toIntArray()))
        assertEquals(3, instance.help.size)
        assertTrue(instance.help.contains("www.help.com"))
        assertTrue(instance.help.contains("www.help2.com"))
        assertTrue(instance.help.contains("www.help3.com"))
    }

    @Test fun `Uses empty list property if missing`() {
        val instance = object {
            val foos by Handler().get<List<Foo>>()
        }
        assertEquals(0, instance.foos.size)
    }

    @Test fun `Delegates promise property to handler`() {
        val foo      = Foo()
        val handler  = Handler().provide(foo)
        val instance = object {
            val foo by handler.getAsync<Foo>()
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
            val foo by handler.getAsync<Foo?>()
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
            val foo by handler.getAsync<List<Foo>>()
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
            val foo by handler.getAsync<Array<Foo>>()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Delegates proxy property to handler`() {
        val handler  = object : Handler(), Auction {
            override fun buy(itemId: Long): UUID = UUID.randomUUID()
        }
        val instance = object {
            @Proxy val auction by handler.get<Auction>()
        }
        assertNotNull(instance.auction.buy(2))
    }

    @Test fun `Delegates container property to handler`() {
        val handler  = TestContainer()
        val instance = object {
            @Managed val foo by handler.get<Foo>()
        }
        assertNotNull(instance.foo)
    }

    @Test fun `Delegates promise container property to handler`() {
        val handler  = TestContainer()
        val instance = object {
            @Managed val foo by handler.getAsync<Foo>()
        }
        assertAsync(testName) { done ->
            instance.foo then {
                assertNotNull(it)
                done()
            }
        }
    }

    @Test fun `Rejects property delegation if missing`() {
        val handler  = Handler()
        val instance = object {
            val foo by handler.get<Foo>()
        }
        assertFailsWith(IllegalStateException::class) {
            instance.foo
        }
    }

    @Test fun `Rejects promise property delegation if missing`() {
        val handler  = Handler()
        val instance = object {
            val foo by handler.getAsync<Foo>()
        }
        assertAsync(testName) { done ->
            instance.foo catch {
                assertTrue(it is IllegalStateException)
                done()
            }
        }
    }
}