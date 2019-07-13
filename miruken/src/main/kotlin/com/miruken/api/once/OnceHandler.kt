package com.miruken.api.once

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.map.map

class OnceHandler
    @Provides @Singleton
    constructor() : Handler() {

    @Handles
    fun once(once: Once, composer: Handling): Promise<*>? {
        val strategy = composer.map<OnceStrategy>(
                once.request, sourceType = once.requestType)
        return strategy?.complete(once, composer)
    }
}