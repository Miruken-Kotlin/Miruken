package com.miruken.api.once

import com.miruken.callback.Handling
import com.miruken.concurrent.Promise

interface OnceStrategy {
    fun complete(once: Once, composer: Handling): Promise<*>
}