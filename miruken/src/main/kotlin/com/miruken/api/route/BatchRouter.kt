package com.miruken.api.route

import com.miruken.api.NamedType
import com.miruken.api.fold
import com.miruken.api.schedule.Concurrent
import com.miruken.api.schedule.Publish
import com.miruken.api.send
import com.miruken.callback.*
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all

class BatchRouter : Handler(), Batching {
    private val _groups = mutableMapOf<String, MutableList<Request>>()

    @Handles
    fun route(routed: Routed, command: Command): Promise<Any?> {
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
                val messages = requests.map { it.message }
                composer.send(Concurrent(messages).routeTo(uri)) then {
                    val responses = it.responses
                    for (i in responses.size until requests.size) {
                        requests[i].promise.cancel()
                    }
                    uri to responses.mapIndexed { index, response ->
                        response.fold({ ex ->
                            requests[index].reject(ex)
                            ex
                        }, { result ->
                            requests[index].resolve(result)
                            result
                        })
                    }
                }
            }
        }).also {
            _groups.clear()
        }

    private class Request(val message: NamedType) {
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