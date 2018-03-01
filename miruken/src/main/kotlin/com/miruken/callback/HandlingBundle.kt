package com.miruken.callback

import com.miruken.concurrent.Promise

fun Handling.bundle(
        all:     Boolean,
        prepare: Bundle.() -> Any?
): HandleResult {
    val bundle = Bundle(all)
    val result = bundle.prepare()
    if (result is HandleResult)
        return result
    if (bundle.isEmpty)
        return HandleResult.HANDLED
    val handled = handle(bundle)
    bundle.complete()
            ?.takeUnless {
                bundle.wantsAsync || !bundle.isAsync }
            ?.get()
    return handled or bundle.handled
}

fun Handling.all(prepare: Bundle.() -> Any?) =
        bundle(true, prepare)

fun Handling.any(prepare: Bundle.() -> Any?) =
        bundle(false, prepare)

fun Handling.bundleAsync(
        all:     Boolean,
        prepare: Bundle.() -> Any?
): Promise<HandleResult> {
    val bundle = Bundle(all).apply { wantsAsync = true}
    val result = bundle.prepare()
    if (result is HandleResult)
        return Promise.resolve(result)
    if (bundle.isEmpty)
        return Promise.resolve(HandleResult.HANDLED)
    val handled = handle(bundle)
    return bundle.complete()!!.then { handled }
}

fun Handling.allAsync(prepare: Bundle.() -> Any?) =
        bundleAsync(true, prepare)

fun Handling.anyAsync(prepare: Bundle.() -> Any?) =
        bundleAsync(false, prepare)