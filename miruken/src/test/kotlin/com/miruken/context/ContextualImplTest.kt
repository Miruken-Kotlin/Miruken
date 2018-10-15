package com.miruken.context

import com.miruken.callback.resolveAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextualImplTest {
    private lateinit var _rootContext: Context

    @Before
    fun setup() {
        _rootContext = Context()
    }

    @After
    fun cleanup() {
        _rootContext.end()
    }

    class MyService : ContextualImpl()

    @Test fun `Adds contextual to context when assigned`() {
        val service  = MyService().apply { context = _rootContext }
        val services = _rootContext.resolveAll<MyService>()
        assertEquals(1, services.size)
        assertTrue(services.contains(service))
    }
}