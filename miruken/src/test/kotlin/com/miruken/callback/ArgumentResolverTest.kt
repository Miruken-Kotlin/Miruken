package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.concurrent.Promise
import com.miruken.concurrent.unwrap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*
import kotlin.test.*

@Suppress("UNUSED_PARAMETER")
class ArgumentResolverTest {
    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
       RepositoryImpl.nextId = 0
    }

    @Test fun `Resolves single dependency`() {
        val handler = (InventoryHandler()
                    + RepositoryImpl<Order>())
        val order   = Order().apply {
            lineItems = listOf(LineItem("1234", 1))
        }
        val confirmation = handler.command<UUID>(NewOrder(order))
        assertNotNull(confirmation)
        assertEquals(1, order.id)
    }

    @Test fun `Fails if unresolved dependency`() {
        assertFailsWith(NotHandledException::class) {
            val handler = InventoryHandler()
            handler.command<UUID>(NewOrder(Order()))
        }
    }

    @Test fun `Resolves promise dependency`() {
        val handler = (InventoryHandler()
                    + RepositoryImpl<Order>())
        val order   = Order().apply { id = 1 }
        assertAsync(testName) { done ->
            handler.commandAsync<Any>(CancelOrder(order)) then {
                done()
            }
        }
    }

    @Test fun `Fails if unresolved promise dependency`() {
        assertFailsWith(NotHandledException::class) {
            val handler = InventoryHandler()
            handler.command<UUID>(CancelOrder(Order()))
        }
    }

    @Test fun `Resolves list dependency`() {
        val handler = (InventoryHandler()
                    + CustomerSupport()
                    + RepositoryImpl<Order>())
        handler.command<Any>(NewOrder(Order()))
        handler.command<Any>(NewOrder(Order()))
        handler.command<Any>(NewOrder(Order()))
        assertAsync(testName) { done ->
            handler.commandAsync<Order>(RefundOrder(orderId = 2)) then {
                assertNotNull(it)
                assertEquals(2, it.id)
                done()
            }
        }
    }

    @Test fun `Rejects command if result is null`() {
        val handler = (InventoryHandler()
                + CustomerSupport()
                + RepositoryImpl<Order>())
        handler.command<Any>(NewOrder(Order()))
        handler.command<Any>(NewOrder(Order()))
        handler.command<Any>(NewOrder(Order()))
        assertAsync(testName) { done ->
            handler.commandAsync<Order>(RefundOrder(orderId = 5)) catch {
                assertTrue(it is NotHandledException)
                done()
            }
        }
    }

    @Test fun `Resolves promise list dependency`() {
        val handler = (InventoryHandler()
                    + CustomerSupport()
                    + RepositoryImpl<Order>())
        handler.command<Any>(NewOrder(Order()))
        handler.command<Any>(NewOrder(Order()))
        assertAsync(testName) { done ->
            handler.commandAsync<List<Order>>(ClockOut()) then {
                assertNotNull(it)
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Resolves simple dependency`() {
        val handler    = (SimpleDependencyHandler()
                       + ConfigurationHandler())
        val maxRetries = handler.command<Int>(NewOrder(Order()))
        assertEquals(2, maxRetries)
    }

    @Test fun `Resolves promise simple dependency`() {
        val handler    = (SimpleDependencyHandler()
                       + ConfigurationHandler())
        val maxRetries = handler.command<Int>(ChangeOrder(Order()))
        assertEquals(2, maxRetries)
    }

    @Test fun `Resolves simple array dependency`() {
        val handler = (SimpleDependencyHandler()
                    + ConfigurationHandler())
        val help    = handler.command<List<String>>(RefundOrder(1))
        assertTrue(help.containsAll(listOf(
                "www.help.com", "www.help2.com", "www.help3.com")))
    }

    @Test fun `Ignores optional missing dependency`() {
        val handler = CustomerSupport()
        assertAsync { done ->
            handler.commandAsync<Any>(NewOrder(Order())) then {
                done()
            }
        }
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
            }.unwrap()
        }
    }

    class CustomerSupport : Handler() {
        @Handles
        fun refundOrder(
                refund:        RefundOrder,
                orders:        List<Order>,
                getRepository: Lazy<Repository<Order>>
        ): Order? {
            return orders.firstOrNull { it.id == refund.orderId }
                    ?.also {
                        it.status = OrderStatus.REFUNDED
                        getRepository.value.save(it)
                    }
        }

        @Handles
        fun clockIn(
                clockIn:    ClockIn,
                orders:     Promise<List<Order>>,
                repository: Repository<Order>
        ): Promise<List<Order>> {
            return orders
        }

        @Handles
        fun clockOut(
                clockOut:   ClockOut,
                orders:     Promise<List<Order>>,
                repository: Repository<Order>
        ): Promise<List<Order>> {
            return orders
        }

        @Handles
        fun validateOrder(
                place:      NewOrder,
                repository: Repository<Order>?) {
            assertNotNull(place)
            assertNull(repository)
        }
    }

    enum class LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        OFF
    }

    class ConfigurationHandler : Handler() {
        @Provides
        val maxRetries: Int = 2

        @Provides
        @Key("logLevel")
        val logLevelInt = LogLevel.INFO

        @Provides
        @Key("logLevelStr")
        val logLevelStr = LogLevel.FATAL.name

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

    class SimpleDependencyHandler : Handler() {
        @Handles
        fun place(
                newOrder:   NewOrder,
                maxRetries: Int,
                @Key("logLevel") logLevel: LogLevel
        ): Int {
            assertEquals(LogLevel.INFO, logLevel)
            return maxRetries
        }

        @Handles
        fun change(
                changeOrder: ChangeOrder,
                maxRetries:  Promise<Int>,
                @Key("logLevel") logLevel: Promise<LogLevel>
        ): Promise<Int> {
            return (logLevel then {
                assertEquals(LogLevel.INFO, it)
                maxRetries
            }).unwrap()
        }

        @Handles
        fun cancel(
                cancelOrder: CancelOrder,
                maxRetries:  Promise<Int>,
                @Key("logLevelStr") logLevel: Promise<LogLevel>
        ): Promise<Int> {
            return (logLevel then {
                assertEquals(LogLevel.FATAL, it)
                maxRetries
            }).unwrap()
        }

        @Handles
        fun refund(
                refundOrder: RefundOrder,
                help: List<String>
        ): List<String> = help
    }
}