package com.miruken.callback

import com.miruken.assertAsync
import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.concurrent.delay
import com.miruken.protocol.proxy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("UNUSED_PARAMETER")
class BundleTest {
    private lateinit var _bowling: BowlingGame

    @Rule
    @JvmField val testName = TestName()

    @Before
    fun setup() {
        _bowling = BowlingGame()
        (0 until 5).forEach {
            _bowling.addHandlers(Lane())
        }
    }

    @Test fun `Handles empty bundle`() {
        assertEquals(HandleResult.HANDLED, _bowling.all {})
    }

    @Test fun `Handles empty bundle async`() {
       assertAsync(testName) { done ->
           _bowling.allAsync {  } then {
               assertEquals(HandleResult.HANDLED, it)
               done()
           }
       }
    }

    @Test fun `Handles single bundle`() {
        var opResult = HandleResult.NOT_HANDLED
        val result   =_bowling.all {
            add { opResult = it.handle(ResetPins()) }
        }
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Handles single bundle async`() {
        var opResult = HandleResult.NOT_HANDLED
        assertAsync(testName) { done ->
            _bowling.allAsync {
                add { opResult = it.handle(ResetPins()) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(HandleResult.HANDLED, opResult)
    }

    @Test fun `Handles single async bundle async`() {
        var opResult = HandleResult.NOT_HANDLED
        assertAsync(testName) { done ->
            _bowling.allAsync {
                addAsync {
                    opResult = it.handle(ResetPins())
                    Promise.EMPTY
                }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(HandleResult.HANDLED, opResult)
    }

    @Test fun `Handles single async bundle`() {
        var opResult = HandleResult.NOT_HANDLED
        val result   =_bowling.all {
            addAsync {
                opResult = it.handle(ResetPins())
                Promise.EMPTY
            }
        }
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Handles multiple bundle`() {
        var opResult = HandleResult.NOT_HANDLED
        val pins     = mutableListOf<Pin>()
        var ball: BowlingBall? = null
        val result   =_bowling.all {
            add { opResult = it.handle(ResetPins()) }
            add { pins.addAll(it.resolveAll()) }
            add { ball = it.command(FindBowlingBall(6.0)) }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(10, pins.size)
        assertEquals(6.0, ball!!.weight)
    }

    @Test fun `Handles multiple bundle async`() {
        var opResult = HandleResult.NOT_HANDLED
        val pins     = mutableListOf<Pin>()
        var ball: BowlingBall? = null
        assertAsync(testName) { done ->
            _bowling.allAsync {
                add { opResult = it.handle(ResetPins()) }
                add { pins.addAll(it.resolveAll()) }
                add { ball = it.command(FindBowlingBall(6.0)) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(10, pins.size)
        assertEquals(6.0, ball!!.weight)
    }

    @Test fun `Handles multiple async bundle async`() {
        var opResult = HandleResult.NOT_HANDLED
        val pins     = mutableListOf<Pin>()
        var ball: BowlingBall? = null
        assertAsync(testName) { done ->
            _bowling.allAsync {
                add { opResult = it.handle(ResetPins()) }
                addAsync {
                    pins.addAll(it.resolveAll())
                    Promise.delay(10)
                }
                add { ball = it.command(FindBowlingBall(6.0)) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(10, pins.size)
        assertEquals(6.0, ball!!.weight)
    }

    @Test fun `Handles multiple async bundle`() {
        var opResult = HandleResult.NOT_HANDLED
        val pins     = mutableListOf<Pin>()
        var ball: BowlingBall? = null
        val result   = _bowling.all {
            addAsync {
                opResult = it.handle(ResetPins())
                Promise.EMPTY
            }
            addAsync {
                pins.addAll(it.resolveAll())
                Promise.delay(10)
            }
            add { ball = it.command(FindBowlingBall(6.0)) }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(HandleResult.HANDLED, opResult)
        assertEquals(10, pins.size)
        assertEquals(6.0, ball!!.weight)
    }

    @Test fun `Handles all bundle`() {
        val bowler = Bowler()
        val result =_bowling.all {
            add { it.handle(ResetPins()) }
            add { it.resolveAll<Pin>() }
            add { it.handle(FindBowlingBall(6.0)) }
            add { it.command<Bowler>(TakeTurn(1, bowler))}
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(1, bowler.frames[0]!!.firstTurn)
        assertEquals(1, bowler.frames[0]!!.secondTurn)
    }

    @Test fun `Handles all bundle async`() {
        val bowler = Bowler()
        assertAsync(testName) { done ->
            _bowling.allAsync {
                add { it.handle(ResetPins()) }
                add { it.resolveAll<Pin>() }
                add { it.handle(FindBowlingBall(6.0)) }
                add { it.command<Bowler>(TakeTurn(1, bowler)) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(1, bowler.frames[0]!!.firstTurn)
        assertEquals(1, bowler.frames[0]!!.secondTurn)
    }

    @Test fun `Reports incomplete bundle`() {
        val result = _bowling.all {
            add { it.handle(ResetPins()) }
            add { it.resolveAll<Pin>() }
            add { it.command(FindBowlingBall(30.0)) }
        }
        assertEquals(HandleResult.NOT_HANDLED, result)
    }

    @Test fun `Reports incomplete bundle async`() {
        assertAsync { done ->
            _bowling.allAsync {
                add { it.handle(ResetPins()) }
                add { it.resolveAll<Pin>() }
                add { it.command(FindBowlingBall(30.0)) }
            } then {
                assertEquals(HandleResult.NOT_HANDLED, it)
                done()
            }
        }
    }

    @Test fun `Handles any bundle`() {
        val result =_bowling.any {
            add { it.handle(FindBowlingBall(30.0)) }
            add { it.resolveAll<Pin>() }
        }
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Handles any bundle async`() {
        assertAsync(testName) { done ->
            _bowling.anyAsync {
                add { it.handle(FindBowlingBall(30.0)) }
                add { it.resolveAll<Pin>() }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
    }

    @Test fun `Supports service provider`() {
        var bowling: Bowling? = null
        val result =_bowling.all {
            add { bowling = it.resolve() }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertSame(_bowling, bowling)
    }

    @Test fun `Supports service provider async`() {
        var bowling: Bowling? = null
        assertAsync(testName) { done ->
            _bowling.allAsync {
                add { bowling = it.resolve() }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
            assertSame(_bowling, bowling)
        }
    }

    @Test fun `Supports single protocol`() {
        var frame: Frame? = null
        val bowler = Bowler()
        assertAsync(testName) { done ->
            _bowling.anyAsync {
                addAsync {
                    it.proxy<Bowling>().bowl(1, bowler) then {
                        frame = it
                    }
                }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                assertEquals(1, frame!!.firstTurn)
                assertEquals(1, frame!!.secondTurn)
                done()
            }
        }
    }

    @Test fun `Propagates protocol exceptions`() {
        assertAsync(testName) { done ->
            _bowling.anyAsync {
                addAsync {
                    it.proxy<Bowling>().bowl(13, Bowler())
                }
            } catch {
                assertTrue(it is IllegalArgumentException)
                assertEquals("Only 11 frames in a bowling game", it.message)
                done()
            }
        }
    }

    @Test fun `Rejects unhandled protocol`() {
        val result = Handler().any {
            add { it.proxy<Bowling>().bowl(13, Bowler()) }
        }
        assertEquals(HandleResult.NOT_HANDLED, result)
    }

    @Test fun `Rejects unhandled protocol async`() {
        assertAsync(testName) { done ->
            Handler().anyAsync {
                add { it.proxy<Bowling>().bowl(13, Bowler()) }
            } then {
                assertEquals(HandleResult.NOT_HANDLED, it)
                done()
            }
        }
    }

    @Test fun `Wraps exceptions in a Promise`() {
        assertAsync(testName) { done ->
            _bowling.anyAsync {
                add { throw IllegalArgumentException("Bad value") }
            } catch {
                assertTrue(it is IllegalArgumentException)
                assertEquals("Bad value", it.message)
                done()
            }
        }
    }

    @Test fun `Supports call semantics`() {
        val result = Handler().bestEffort.any {
            add { it.proxy<Bowling>().bowl(8, Bowler()) }
        }
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Supports call semantics async`() {
        assertAsync(testName) { done ->
            Handler().bestEffort.anyAsync {
                add { it.proxy<Bowling>().bowl(13, Bowler()) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
    }

    @Test fun `Supports inner call semantics`() {
        val result = Bowler().toHandler().any {
            add { it.bestEffort.proxy<Bowling>().bowl(8, Bowler()) }
        }
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Handles multiple all greedily`() {
        var handled = 0
        val pins    = mutableListOf<Pin>()
        val result  = _bowling.broadcast.all {
            add { handled += when (it.handle(ResetPins())) {
                HandleResult.HANDLED -> 1
                else -> 0
            }}
            add { pins.addAll(it.resolveAll()) }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(5, handled)
        assertEquals(50, pins.size)
    }

    @Test fun `Handles multiple all greedily async`() {
        var handled = 0
        val pins    = mutableListOf<Pin>()
        assertAsync { done ->
            _bowling.broadcast.allAsync {
                add { handled += when (it.handle(ResetPins())) {
                    HandleResult.HANDLED -> 1
                    else -> 0
                }}
                add { pins.addAll(it.resolveAll()) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(5, handled)
        assertEquals(50, pins.size)
    }

    @Test fun `Handles multiple any greedily`() {
        var handled = 0
        val pins    = mutableListOf<Pin>()
        val result  = _bowling.broadcast.any {
            add { handled += when (it.handle(ResetPins())) {
                HandleResult.HANDLED -> 1
                else -> 0
            }}
            add { pins.addAll(it.resolveAll()) }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(5, handled)
        assertEquals(50, pins.size)
    }

    @Test fun `Handles multiple any greedily async`() {
        var handled = 0
        val pins    = mutableListOf<Pin>()
        assertAsync { done ->
            _bowling.broadcast.anyAsync {
                add { handled += when (it.handle(ResetPins())) {
                    HandleResult.HANDLED -> 1
                    else -> 0
                }}
                add { pins.addAll(it.resolveAll()) }
            } then {
                assertEquals(HandleResult.HANDLED, it)
                done()
            }
        }
        assertEquals(5, handled)
        assertEquals(50, pins.size)
    }

    @Test fun `Supports resolving bundles`() {
        var handled = HandleResult.NOT_HANDLED
        val pins    = mutableListOf<Pin>()
        var ball: BowlingBall? = null
        HandlerDescriptor.getDescriptor<BowlingGame>()
        HandlerDescriptor.getDescriptor<Lane>()
        val result = BowlingProvider().resolving().all {
            add { handled = it.handle(ResetPins()) }
            add { pins.addAll(it.resolveAll()) }
            add { ball = it.command(FindBowlingBall(10.0)) }
        }
        assertEquals(HandleResult.HANDLED, result)
        assertEquals(HandleResult.HANDLED, handled)
        assertEquals(10, pins.size)
        assertEquals(10.0, ball!!.weight)
    }

    class BowlingProvider : Handler() {
        @Provides
        val bowling = BowlingGame()

        @Provides
        val lanes = (1..5).map { Lane(true) }
    }

    class Pin(val number: Int) {
        var up: Boolean = false
    }

    class Frame {
        var firstTurn:  Int? = null
        var secondTurn: Int? = null
    }

    class BowlingBall(val weight: Double)

    class Bowler {
        val frames = arrayOfNulls<Frame>(11)
    }

    class Lane(private val resolve: Boolean = false) {
        @Provides val pins get() =
            (1..10).map { Pin(it).apply { up = true } }

        @Handles
        fun reset(reset: ResetPins, composer: Handling) {
            assertNotNull(composer)
            for (pin in pins) pin.up = true
            (composer.takeUnless { resolve } ?: composer.resolving())
                    .command<BowlingBall>(FindBowlingBall(5.0))
        }
    }

    class FindBowlingBall(val weight: Double)

    class TakeTurn(val frame: Int, val bowler: Bowler)

    class ResetPins

    interface Bowling {
        fun bowl(frame: Int, bowler: Bowler): Promise<Frame>
        fun getScore(bowler: Bowler): Int
    }

    class BowlingGame : CompositeHandler(), Bowling {
        @Handles
        fun findBall(findBall: FindBowlingBall): BowlingBall? =
                findBall.takeIf { it.weight < 20.0 }
                        ?.let { BowlingBall(it.weight) }

        @Handles
        fun takeTurn(turn: TakeTurn, composer: Handling): Promise<Bowler> {
            val frame  = turn.frame
            val bowler = turn.bowler
            bowler.frames[frame - 1] = Frame().apply {
                firstTurn  = frame
                secondTurn = frame
            }
            composer.proxy<Bowling>().getScore(turn.bowler)
            return Promise.resolve(bowler)
        }

        override fun bowl(frame: Int, bowler:Bowler): Promise<Frame> {
            require(frame <= 11) { "Only 11 frames in a bowling game" }
            val newFrame = Frame().apply {
                firstTurn  = frame
                secondTurn = frame
            }
            bowler.frames[frame - 1] = newFrame
            return Promise.resolve(newFrame)
        }

        override fun getScore(bowler: Bowler) =
                bowler.frames.filterNotNull()
                        .sumBy { (it.firstTurn ?: 0) + (it.secondTurn ?: 0) }

    }
}