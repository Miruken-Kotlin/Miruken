package com.miruken.api.once

import com.miruken.api.send
import com.miruken.callback.Handling
import com.miruken.callback.with

object DelegatingOnceStrategy : OnceStrategy {
    override fun complete(once: Once, composer: Handling) =
            composer.with(once).send(once.request, once.requestType)
}