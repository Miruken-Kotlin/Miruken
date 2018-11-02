package com.miruken.api.route

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf
import kotlin.reflect.KType

interface Routed {
    val message:     NamedType
    val messageType: KType?
    val route:       String
    val tag:         String?
}

data class RoutedMessage(
        override val message:     NamedType,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null
) : Routed

data class RoutedRequest<TResp: Any>(
        override val message:     Request<TResp>,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null,
        override val typeName: String =
                "Miruken.Mediate.Route.RoutedRequest`1[[${message.typeName}]],Miruken.Mediate"
) : Request<TResp>, Routed

inline fun <reified T: NamedType>
        T.routeTo(route: String, tag: String? = null
): RoutedMessage = RoutedMessage(this, typeOf<T>(), route, tag)

inline fun <TResp: Any, reified T: Request<TResp>>
        T.routeTo(route: String, tag: String? = null
): RoutedRequest<TResp> = RoutedRequest(this, typeOf<T>(), route, tag)