package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.getKType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class CommandTest {
    class Foo
    class Bar<out T>

    @Test
    fun `Creates Command of String`() {
        val command = Command(Foo(), getKType<String>())
        assertFalse(command.many)
        assertFalse(command.wantsAsync)
        assertEquals(getKType<String>(), command.resultType)
        assertSame(HandlesPolicy, command.policy)
    }

    @Test
    fun `Creates Command of KClass`() {
        val command = Command(Foo(), getKType<Foo>())
        assertEquals(getKType<Foo>(), command.resultType)
    }

    @Test
    fun `Creates Command of generic KClass`() {
        val command = Command(Bar<Int>(), getKType<Bar<Int>>())
        assertEquals(getKType<Bar<Int>>(), command.resultType)
    }

    @Test
    fun `Creates many Command of KType`() {
        val command = Command(Bar<String>(), getKType<Bar<String>>(), true)
        assertEquals(getKType<List<Bar<String>>>(), command.resultType)
    }

    @Test
    fun `Creates async Command of KType`() {
        val command = Command(Bar<String>(), getKType<Bar<String>>()).apply {
            wantsAsync = true
        }
        assertEquals(getKType<Promise<Bar<String>>>(), command.resultType)
    }

    @Test
    fun `Creates many async Command of KType`() {
        val command = Command(Bar<Int>(), getKType<Bar<Int>>(), true).apply {
            wantsAsync = true
        }
        assertEquals(getKType<Promise<List<Bar<Int>>>>(), command.resultType)
    }
}
    
