package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class CommandTest {
    class Foo
    class Bar<out T>

    @Test fun `Creates Command of String`() {
        val command = Command(Foo(), typeOf<String>())
        assertFalse(command.many)
        assertFalse(command.wantsAsync)
        assertEquals(typeOf<String>(), command.resultType)
        assertSame(HandlesPolicy, command.policy)
    }

    @Test fun `Creates Command of KClass`() {
        val command = Command(Foo(), typeOf<Foo>())
        assertEquals(typeOf<Foo>(), command.resultType)
    }

    @Test fun `Creates Command of generic KClass`() {
        val command = Command(Bar<Int>(), typeOf<Bar<Int>>())
        assertEquals(typeOf<Bar<Int>>(), command.resultType)
    }

    @Test fun `Creates many Command of KType`() {
        val command = Command(Bar<String>(), typeOf<Bar<String>>(), true)
        assertEquals(typeOf<List<Bar<String>>>(), command.resultType)
    }

    @Test fun `Creates async Command of KType`() {
        val command = Command(Bar<String>(), typeOf<Bar<String>>()).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Promise<Bar<String>>>(), command.resultType)
    }

    @Test fun `Creates many async Command of KType`() {
        val command = Command(Bar<Int>(), typeOf<Bar<Int>>(), true).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Promise<List<Bar<Int>>>>(), command.resultType)
    }
}
    
