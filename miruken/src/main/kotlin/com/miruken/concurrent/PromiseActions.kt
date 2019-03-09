package com.miruken.concurrent

/*
inline infix fun <R, reified A> Promise<Handling>.then(
        crossinline action: Handling.(a: A) -> R
): Promise<R?> = then { it.execute(action).second }
*/