package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.concurrent.unwrap
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArgumentResolverTest {

    @Before
    fun setup() {
       RepositoryImpl.nextId = 0
    }

    @Test fun `Resolves single dependency`() {
        val handler = InventoryHandler() + RepositoryImpl<Order>()
        val order   = Order().apply {
            lineItems = listOf(LineItem("1234", 1))
        }

        val confirmation = handler.command<UUID>(NewOrder(order))
        assertNotNull(confirmation)
        assertEquals(1, order.id)
    }

    interface Entity {
        var id: Int
    }

    enum class OrderStatus {
        PENDING,
        CANCELLED,
        REFUNDED
    }

    data class LineItem(
            var PLU:      String? = null,
            var quantity: Int? = null
    )

    data class Order(
            override var id: Int             = 0,
            var status:      OrderStatus?    = null,
            var lineItems:   List<LineItem>? = null)
        : Entity


    data class NewOrder(val order: Order)

    data class ChangeOrder(val order: Order)

    data class RefundOrder(val orderId: Int)

    data class CancelOrder(val order: Order)

    class ClockIn

    class ClockOut

    interface Repository<in T: Entity> {
        fun save(entity: T): Promise<Any?>
    }

    class RepositoryImpl<in T: Entity> : Repository<T> {
        override fun save(entity: T): Promise<Any?> {
            if (entity.id <= 0)
                entity.id = ++nextId
            return Promise.EMPTY
        }

        companion object {
            var nextId: Int = 0
        }
    }
    class InventoryHandler : Handler() {

        private val _orders = mutableListOf<Order>()

        @Provides
        val order get() = _orders.toList()

        @Handles
        fun placeOrder(
                place:      NewOrder,
                repository: Repository<Order>,
                getOrders:  Lazy<List<Order>>
        ): UUID {
            val order    = place.order
            order.status = OrderStatus.PENDING
            repository.save(order)
            _orders.add(order)
            val orders = getOrders.value
            assertTrue(orders.contains(order))
            return UUID.randomUUID()
        }

        @Handles
        fun changeOrder(
                change:     ChangeOrder,
                repository: Promise<Repository<Order>>,
                getOrders:  () -> Promise<List<Order>>
        ): Promise<UUID> {
            change.order.status = OrderStatus.PENDING
            return repository.then {
                it.save(change.order)
                getOrders() then {
                    assertEquals(0, it.size)
                    UUID.randomUUID()
                }
            }.unwrap()
        }

        @Handles
        fun cancelOrder(
                cancel:     CancelOrder,
                repository: Promise<Repository<Order>>,
                getOrders:  Lazy<Promise<List<Order>>>
        ): Promise<Any> {
            cancel.order.status = OrderStatus.CANCELLED
            return getOrders.value.then {
                assertEquals(0, it.size)
                repository then {
                    it.save(cancel.order)
                }
            }
        }
    }

    /*
    private class CustomerSupport : Handler
    {
        [Handles]
        public Order RefundOrder(RefundOrder refund, Order[] orders,
        Func<IRepository<Order>> getRepository)
        {
            var order = orders.FirstOrDefault(o => o.Id == refund.OrderId);
            if (order != null)
            {
                order.Status = OrderStatus.Refunded;
                getRepository().Save(order);
            }
            return order;
        }

        [Handles]
        public Promise<Order[]> ClockIn(ClockIn clockIn, Task<Order[]> orders,
        IRepository<Order> repository)
        {
            return orders;
        }

        [Handles]
        public Task<Order[]> ClockOut(ClockOut clockOut, Promise<Order[]> orders,
        IRepository<Order> repository)
        {
            return orders;
        }

        [Handles]
        public void ValidateOrder(NewOrder place, [Optional]IRepository<Order> repository)
        {
            Assert.IsNotNull(place);
            Assert.IsNull(repository);
        }
    }
    */
}