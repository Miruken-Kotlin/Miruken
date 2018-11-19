package com.miruken.mvc

import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

@Protocol
interface NavigatingAware {
    fun navigating(navigation: Navigation<*>) {}

    fun navigatingOut(navigation: Navigation<*>) {}

    companion object {
        val PROTOCOL = typeOf<NavigatingAware>()
        operator fun invoke(adapter: ProtocolAdapter?) =
                adapter?.proxy(PROTOCOL) as NavigatingAware
    }
}