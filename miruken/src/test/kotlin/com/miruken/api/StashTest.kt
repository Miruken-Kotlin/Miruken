package com.miruken.api

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.test.assertAsync
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class StashTest {
    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<OrderHandler>()
        factory.registerDescriptor<StashImpl>()
        HandlerDescriptorFactory.useFactory(factory)
    }

    @Test fun `Adds to stash`() {
        val order   = Order()
        val handler = StashImpl()
        handler.stashPut(order)
        assertSame(order, handler.stashGet()!!)
    }

    @Test fun `Get or adds to stash`() {
        val order   = Order()
        val handler = StashImpl()
        val result  = handler.stashGetOrPut(order)
        assertSame(order, result)
        assertSame(order, handler.stashGet()!!)
    }

    @Test fun `Get or adds to stash async`() {
        val order   = Order()
        val handler = StashImpl()
        val result  = handler.stashGetOrPutAsync {
            Promise.resolve(order)
        }
        assertSame(order, result)
        assertSame(order, handler.stashGet()!!)
    }

    @Test fun `Get or adds to stash suspend`() {
        val order   = Order()
        val handler = StashImpl()
        val result  = handler.stashGetOrPutCo {
            delay(10)
            order
        }
        assertSame(order, result)
        assertSame(order, handler.stashGet()!!)
    }

    @Test fun `Drops from stash`() {
        val order   = Order()
        val handler = StashImpl() + StashImpl(true)
        handler.stashPut(order)
        handler.stashDrop<Order>()
        assertNull(handler.stashGet<Order>())
    }

    @Test fun `Cascades stash`() {
        val order    = Order()
        val handler  = StashImpl()
        val handler2 = StashImpl() + handler
        handler.stashPut(order)
        assertSame(order, handler2.stashGet()!!)
    }

    @Test fun `Accesses stash`() {
        val handler = OrderHandler()
        assertAsync(testName) { done ->
            handler.send(CancelOrder(1)) then { order ->
                assertEquals(1, order.id)
                assertEquals(OrderStatus.CANCELLED, order.status)
                done()
            }
        }
    }

    enum class OrderStatus {
        CREATED,
        CANCELLED
    }

    data class Order(
        var id:     Int?        = null,
        var status: OrderStatus = OrderStatus.CREATED,
        override val typeName: String = "Order"
    ): NamedType

    data class CancelOrder(
            val orderId: Int,
            override val typeName: String = ""
    ): Request<Order>

    class OrderHandler : Handler() {
        @Handles
        @CancelOrderIntegrity
        fun cancel(
                cancel: CancelOrder,
                order:  Order
        ): Order {
            assertEquals(cancel.orderId, order.id)
            order.status = OrderStatus.CANCELLED
            return order
        }
    }

    object CancelOrderIntegrityFilter : Filtering<CancelOrder, Order> {
        override var order: Int? = null

        override fun next(
                callback:    CancelOrder,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Order>,
                provider:    FilteringProvider?
        ): Promise<Order> {
            composer.stashPut(Order().apply {
                id = callback.orderId
            })
            return next()
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    @UseFilter(CancelOrderIntegrityFilter::class)
    annotation class CancelOrderIntegrity
}