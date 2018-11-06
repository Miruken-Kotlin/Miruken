package com.miruken.api.route

import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.typeOf
import kotlin.reflect.KType

interface Routed : NamedType{
    val message:     NamedType
    val messageType: KType?
    val route:       String
    val tag:         String?
}

data class RoutedMessage(
        override val message:     NamedType,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null,
        override val typeName:    String =
                RoutedMessage.typeName.format(message.typeName)
) : Routed {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Route.RoutedMessage`1[[%s]],Miruken.Mediate"
    }
}

data class RoutedRequest<TResp: NamedType>(
        override val message:     Request<TResp>,
        override val messageType: KType?,
        override val route:       String,
        override val tag:         String? = null,
        override val typeName:    String =
                RoutedRequest.typeName.format(message.typeName)
) : Request<TResp>, Routed {
    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Route.RoutedRequest`1[[%s]],Miruken.Mediate"
    }
}

inline fun <reified T: NamedType>
        T.routeTo(route: String, tag: String? = null
): RoutedMessage = RoutedMessage(this, typeOf<T>(), route, tag)

inline fun <TResp: NamedType, reified T: Request<TResp>>
        T.routeTo(route: String, tag: String? = null
): RoutedRequest<TResp> = RoutedRequest(this, typeOf<T>(), route, tag)