package com.miruken.api

import com.miruken.assertAsync
import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.protocol.proxy
import org.junit.Before
import org.junit.Ignore
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
       HandlerDescriptor.resetDescriptors()
    }

    @Test fun `Adds to stash`() {
        val order   = Order()
        val handler = StashImpl()
        val stash   = handler.proxy<Stash>()
        stash.put(order)
        assertSame(order, stash.get()!!)
    }

    @Test fun `Get or adds to stash`() {
        val order   = Order()
        val handler = StashImpl()
        val stash   = handler.proxy<Stash>()
        val result  = stash.getOrPut(order)
        assertSame(order, result)
        assertSame(order, stash.get()!!)
    }

    @Test fun `Drops from stash`() {
        val order   = Order()
        val handler = StashImpl() + StashImpl(true)
        val stash   = handler.proxy<Stash>()
        stash.put(order)
        stash.drop<Order>()
        assertNull(stash.get<Order>())
    }

    @Test fun `Cascades stash`() {
        val order    = Order()
        val handler  = StashImpl()
        val handler2 = StashImpl() + handler
        handler.proxy<Stash>().put(order)
        assertSame(order, handler2.proxy<Stash>().get()!!)
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
                callback: CancelOrder,
                binding:  MemberBinding,
                composer: Handling,
                next:     Next<Order>,
                provider: FilteringProvider?
        ): Promise<Order> {
            composer.proxy<Stash>().put(Order().apply {
                id = callback.orderId
            })
            return next()
        }
    }

    @Target(AnnotationTarget.FUNCTION)
    @UseFilter(CancelOrderIntegrityFilter::class)
    annotation class CancelOrderIntegrity
}