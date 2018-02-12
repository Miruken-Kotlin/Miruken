package com.miruken.callback

import org.junit.*
import kotlin.test.assertSame

class HandleResultTest {
    @Test fun `HANDLED follows logic table`() {
        val result = HandleResult.HANDLED
        assertSame(HandleResult.HANDLED, result + HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.HANDLED, result + HandleResult.NOT_HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `HANDLED_AND_STOP follows logic table`() {
        val result = HandleResult.HANDLED_AND_STOP
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.NOT_HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `NOT_HANDLED follows logic table`() {
        val result = HandleResult.NOT_HANDLED
        assertSame(HandleResult.HANDLED, result + HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result + HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED, result + HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result + HandleResult.NOT_HANDLED_AND_STOP)
    }
}