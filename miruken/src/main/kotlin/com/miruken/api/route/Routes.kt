package com.miruken.api.route

import com.miruken.Stage
import com.miruken.api.NamedType
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise

class RoutesFilter<Res: NamedType>(
        vararg val schemes: String
) : Filtering<RoutedRequest<Res>, Res> {

    override var order: Int? = Stage.LOGGING - 1

    override fun next(
            callback: RoutedRequest<Res>,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ): Promise<Res> {
        return next()
    }
}

object RoutesFactory : FilteringProviderFactory {
    override fun createProvider(
            annotation: Annotation
    ): FilteringProvider {
        val routes = annotation as Routes
        require(routes.schemes.isNotEmpty()) {
            "Schemes cannot be empty"
        }
        return FilterInstanceProvider(
            RoutesFilter<NamedType>(*routes.schemes))
    }
}

@UseFilterProviderFactory(RoutesFactory::class)
annotation class Routes(vararg val schemes: String)