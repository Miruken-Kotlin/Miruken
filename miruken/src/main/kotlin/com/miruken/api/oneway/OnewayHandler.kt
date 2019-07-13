package com.miruken.api.oneway

import com.miruken.api.NamedType
import com.miruken.api.send
import com.miruken.callback.*

class OnewayHandler
    @Provides @Singleton
    constructor() : Handler() {

    @Handles
    fun <TResp: NamedType> oneway(oneway: Oneway, composer: Handling) =
            composer.send(oneway.request, oneway.requestType)
}