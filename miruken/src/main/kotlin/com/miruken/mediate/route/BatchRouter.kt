package com.miruken.mediate.route

import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.mediate.schedule.Publish
import com.miruken.mediate.send

class BatchRouter : Handler(), Batching {
    private val _groups = mutableMapOf<String, MutableList<Request>>()

    @Handles
    fun route(
            routed:  Routed,
            command: Command
    ): Promise<Any?> {
        val route   = routed.route
        val message = routed.message
                .takeUnless { command.many } ?:
            Publish(routed.message)
        val request = Request(message)
        if (_groups[route]?.add(request) == null) {
            _groups[route] = mutableListOf(request)
        }
        return request.promise
    }

    override fun complete(composer: Handling): Any? =
        Promise.all(_groups.map { group ->
            val uri      = group.key
            val requests = group.value
            if (requests.size == 1) {
                requests[0].let { request ->
                    composer.send(request.message.routeTo(uri)) then {
                        request.resolve(it)
                        uri to listOf(it)
                    } catch { error ->
                        request.reject(error)
                        uri to listOf(error)
                    }
                }
            } else {

            }
        })

    private class Request(val message: Any) {
        lateinit var resolve: (Any?) -> Unit
            private set
        lateinit var reject:  (Throwable) -> Unit
            private set

        val promise = Promise<Any?> { resolve, reject ->
            this.resolve = resolve
            this.reject  = reject
        }
    }
}