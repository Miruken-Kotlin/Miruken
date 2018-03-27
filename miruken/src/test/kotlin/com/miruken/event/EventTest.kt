package com.miruken.event

import org.junit.Test
import kotlin.test.assertEquals

class EventTest {
    data class Activity(val user: String, val action: String)

    @Test fun `Can create simple event`() {
        var count = 0
        val event = Event<Int>()
        event += { n -> count += n }
        event += { n -> count += n }
        event(2)
        event(3)
        assertEquals(10, count)
    }

    @Test fun `Can create complex event`() {
        val event = Event<Activity>()
        val audit = mutableListOf<String>()
        event += { (user, action) -> audit.add("$user:$action")}
        event(Activity("johns24", "download"))
        event(Activity("sarah15", "login"))
        assertEquals(listOf("johns24:download", "sarah15:login"), audit)
    }

    @Test fun `Can create complex event lazily`() {
        val event = Event<Activity>()
        val audit = mutableListOf<String>()
        event += { (user, action) -> audit.add("$user:$action")}
        event { Activity("johns24", "download") }
        event { Activity("sarah15", "login") }
        assertEquals(listOf("johns24:download", "sarah15:login"), audit)
    }

    @Test fun `Can register and unregister from event`() {
        val event      = Event<String>()
        val trace      = mutableListOf<String>()
        val unregister = event register { s -> trace.add(s) }
        event("Starting App...")
        unregister()
        event("Terminating App...")
        assertEquals(listOf("Starting App..."), trace)
    }

    @Test fun `Can clear all event handlers`() {
        val event      = Event<String>()
        val trace      = mutableListOf<String>()
        val unregister = event register { s -> trace.add(s) }
        event("Starting App...")
        event.clear()
        event("Terminating App...")
        unregister()
        assertEquals(listOf("Starting App..."), trace)
    }
}