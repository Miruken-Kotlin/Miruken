package com.miruken.api.route

import com.miruken.TypeReference
import com.miruken.api.MessageWrapper
import com.miruken.api.NamedType
import com.miruken.api.Request
import com.miruken.api.responseTypeName
import com.miruken.typeOf

interface Routed : MessageWrapper {
    val route: String
    val tag:   String?
}

data class RoutedMessage(
        override val message:     NamedType,
        override val messageType: TypeReference?,
        override val route:       String,
        override val tag:         String? = null
) : Routed {
    override val typeName: String = RoutedMessage.typeName

    companion object : NamedType {
        override val typeName =
                "Miruken.Mediate.Route.Routed,Miruken.Mediate"
    }
}

data class RoutedRequest<TResp: NamedType>(
        override val message:     Request<TResp>,
        override val messageType: TypeReference?,
        override val route:       String,
        override val tag:         String? = null
) : Request<TResp>, Routed {
    override val typeName: String  =
            RoutedRequest.typeName.format(message.responseTypeName)

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