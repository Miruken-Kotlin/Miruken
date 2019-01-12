package com.miruken.api.route

import com.miruken.Stage
import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import java.net.URI

class RoutesFilter<Res: Any?>(vararg val schemes: String) : Filtering<Routed, Res> {
    override var order: Int? = Stage.LOGGING - 1

    override fun next(
            callback: Routed,
            binding:  MemberBinding,
            composer: Handling,
            next:     Next<Res>,
            provider: FilteringProvider?
    ): Promise<Res> {
        if (schemes.indexOf(getScheme(callback.route)) >= 0) {
            val batcher = composer.getBatcher(null) { BatchRouter() }
            if (batcher != null) {
                @Suppress("UNCHECKED_CAST")
                return composer.enableFilters().commandAsync(Batched(callback)) as Promise<Res>
            }
            return next(composer.enableFilters())
        }
        return next.abort()
    }

    private fun getScheme(route: String) = try {
        URI(route).run { scheme ?: path }
    } catch (t: Throwable) {
        null
    }
}

object RoutesFactory : FilteringProviderFactory {
    override fun createProvider(annotation: Annotation): FilteringProvider {
        val routes = annotation as Routes
        require(routes.schemes.isNotEmpty()) {
            "Schemes cannot be empty"
        }
        return FilterInstanceProvider(RoutesFilter<Any?>(*routes.schemes))
    }
}

@UseFilterProviderFactory(RoutesFactory::class)
annotation class Routes(vararg val schemes: String)