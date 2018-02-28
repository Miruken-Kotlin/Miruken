package com.miruken.callback

import com.miruken.concurrent.Promise

fun Handling.bundle(
        all:     Boolean,
        prepare: Bundle.() -> Unit
): HandleResult {
    val bundle = Bundle(all)
    bundle.prepare()
    if (bundle.isEmpty)
        return HandleResult.HANDLED
    val handled = handle(bundle)
    bundle.complete()
            ?.takeUnless {
                bundle.wantsAsync || !bundle.isAsync }
            ?.get()
    return handled or bundle.handled
}

fun Handling.all(prepare:Bundle.() -> Unit) =
        bundle(true, prepare)

fun Handling.any(prepare:Bundle.() -> Unit) =
        bundle(false, prepare)

fun Handling.bundleAsync(
        all:     Boolean,
        prepare: Bundle.() -> Unit
): Promise<HandleResult> {
    val bundle = Bundle(all).apply { wantsAsync = true}
    bundle.prepare()
    if (bundle.isEmpty)
        return Promise.resolve(HandleResult.HANDLED)
    val handled = handle(bundle)
    return bundle.complete()!!.then { handled }
}

fun Handling.allAsync(prepare:Bundle.() -> Unit) =
        bundleAsync(true, prepare)

fun Handling.anyAsync(prepare:Bundle.() -> Unit) =
        bundleAsync(false, prepare)