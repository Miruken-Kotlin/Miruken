package com.miruken.callback

import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.test.assertAsync
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HandlerCreateTest {
    private lateinit var _handler: Handling

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        _handler = TypeHandlers
        HandlerDescriptorFactory.useFactory(
                MutableHandlerDescriptorFactory().apply {
            registerDescriptor<ViewFactory>()
            registerDescriptor<ControllerImpl>()
            registerDescriptor<Warehouse>()
        })
    }

    @Test fun `Creates instance`() {
        val controller = _handler.create<ControllerImpl>()
        assertNotNull(controller)
        assertNotNull(controller.viewFactory)
    }

    @Test fun `Creates instance from interface`() {
        val controller = _handler.create<Controller>()
        assertNotNull(controller)
        assertTrue(controller is ControllerImpl)
        assertNotNull(controller.viewFactory)
    }

    @Test fun `Creates instance asynchronously`() {
        assertAsync(testName) { done ->
            _handler.createAsync<ControllerImpl>() then {
                assertNotNull(it)
                assertNotNull(it.viewFactory)
                done()
            }
        }
    }

    @Test fun `Creates all instances`() {
        val controllers = _handler.createAll<Controller>()
        assertEquals(1, controllers.size)
        assertNotNull(controllers.first())
    }

    @Test fun `Creates all instances asynchronously`() {
        assertAsync(testName) { done ->
            _handler.createAllAsync<ControllerImpl>() then {
                assertEquals(1, it.size)
                assertNotNull(it.first())
                done()
            }
        }
    }

    @Test fun `Resolves instance`() {
        val controller = _handler.resolve<ControllerImpl>()
        assertNotNull(controller)
        assertNotNull(controller.viewFactory)
    }

    @Test fun `Rejects unhandled creation`() {
        assertFailsWith(NotHandledException::class) {
            _handler.create<ViewFactory>()
        }
    }

    @Test fun `Rejects unhandled creation asynchronously`() {
        assertAsync(testName) { done ->
            _handler.createAsync<ViewFactory>() catch {
                assertTrue(it is NotHandledException)
                done()
            }
        }
    }

    @Test fun `Rejects creation if missing dependencies`() {
        assertFailsWith(NotHandledException::class) {
            _handler.create<Warehouse>()
        }
    }

    class ViewFactory @Provides constructor()

    interface Controller
    interface Delivery

    class ControllerImpl
        @Creates
        @Provides
        constructor(
                val viewFactory : ViewFactory
        ) : Controller

    class Warehouse
        @Creates
        constructor(
                val delivery: Delivery
        )
}