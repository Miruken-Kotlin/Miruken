package com.miruken.callback

import com.miruken.concurrent.Promise
import kotlin.test.assertNotNull

@Suppress("UNUSED_PARAMETER")
class BundleTest {

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
            (0..10).map { Pin(it).apply { up = true } }

        @Handles
        fun reset(reset: ResetPins, composer: Handling) {
            assertNotNull(composer)
            for (pin in pins) pin.up = true
            (composer.takeUnless { resolve } ?: composer)
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

    private class BowlingGame : CompositeHandler(), Bowling {
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
            //val scope = Proxy<IBowling>(composer).GetScore(turn.Bowler)
            return Promise.resolve(bowler)
        }

        override fun bowl(frame: Int, bowler:Bowler): Promise<Frame>
        {
            require(frame > 11)

            var newFrame = Frame().apply {
                firstTurn  = frame
                secondTurn = frame
            }
            bowler.frames[frame - 1] = newFrame
            return Promise.resolve(newFrame)
        }

        override fun getScore(bowler: Bowler): Int =
                bowler.frames
                        .filterNotNull()
                        .sumBy { (it.firstTurn ?: 0) + (it.secondTurn ?: 0) }

    }
}