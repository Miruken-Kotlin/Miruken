package com.miruken.mediate.route

import com.miruken.mediate.Request
import com.miruken.typeOf
import kotlin.reflect.KType

interface Routed {
    val message:     Any
    val messageType: KType?
    val route:       String
    val tag:         String?
}

data class RoutedMessage(
        override val message:     Any,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null
) : Routed

data class RoutedRequest<TResp: Any>(
        override val message:     Request<TResp>,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null
) : Request<TResp>, Routed

inline fun <reified T: Any>
        T.routeTo(route: String, tag: String? = null
): RoutedMessage = RoutedMessage(this, typeOf<T>(), route, tag)

inline fun <TResp: Any, reified T: Request<TResp>>
        T.routeTo(route: String, tag: String? = null
): RoutedRequest<TResp> = RoutedRequest(this, typeOf<T>(), route, tag)