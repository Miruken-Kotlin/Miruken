package com.miruken.api.route

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.send
import com.miruken.callback.Handling
import com.miruken.concurrent.Promise
import com.miruken.context.Contextual
import com.miruken.context.requireContext
import com.miruken.typeOf

class RoutedApi(val route: String) {
    inline fun <reified T: NamedType> send(
            handler: Handling, request: T) =
            handler.send(request.routeTo(route))

    inline fun <reified T: NamedType> send(
            contextual: Contextual, request: T) =
            contextual.requireContext().send(request.routeTo(route))

    //inline fun <TResp: Any?, reified T: Request<TResp>>
    //        Handling.send(handler: Handling, request: T): Promise<TResp> =
    //        handler.send(request.routeTo(route))
}