package com.miruken.api.oneway

import com.miruken.callback.Handler
import com.miruken.callback.Handles
import com.miruken.callback.Handling
import com.miruken.api.send

class OnewayHandler : Handler() {
    @Handles
    fun <TResponse: Any> oneway(
            request:  Oneway<TResponse>,
            composer: Handling
    ) = composer.send(request.request, request.requestType)
}