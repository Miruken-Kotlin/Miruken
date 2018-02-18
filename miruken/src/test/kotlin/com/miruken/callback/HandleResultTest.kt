package com.miruken.callback

import org.junit.*
import kotlin.test.assertSame

class HandleResultTest {
    @Test fun `HANDLED follows or logic table`() {
        val result = HandleResult.HANDLED
        assertSame(HandleResult.HANDLED, result or HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.HANDLED, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `HANDLED_AND_STOP follows or logic table`() {
        val result = HandleResult.HANDLED_AND_STOP
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `NOT_HANDLED follows or logic table`() {
        val result = HandleResult.NOT_HANDLED
        assertSame(HandleResult.HANDLED, result or HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `NOT_HANDLED_AND_STOP follows or logic table`() {
        val result = HandleResult.NOT_HANDLED_AND_STOP
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result or HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `HANDLED follows and logic table`() {
        val result = HandleResult.HANDLED
        assertSame(HandleResult.HANDLED, result and HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result and HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED, result and HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `HANDLED_AND_STOP follows and logic table`() {
        val result = HandleResult.HANDLED_AND_STOP
        assertSame(HandleResult.HANDLED_AND_STOP, result and HandleResult.HANDLED)
        assertSame(HandleResult.HANDLED_AND_STOP, result and HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `NOT_HANDLED follows and logic table`() {
        val result = HandleResult.NOT_HANDLED
        assertSame(HandleResult.NOT_HANDLED, result and HandleResult.HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }

    @Test fun `NOT_HANDLED_AND_STOP follows and logic table`() {
        val result = HandleResult.NOT_HANDLED_AND_STOP
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result and HandleResult.HANDLED_AND_STOP)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED)
        assertSame(HandleResult.NOT_HANDLED_AND_STOP, result or HandleResult.NOT_HANDLED_AND_STOP)
    }
}