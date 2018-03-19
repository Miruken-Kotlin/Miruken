package com.miruken.context

import com.miruken.callback.Handling
import com.miruken.callback.resolve
import com.miruken.callback.publish
import com.miruken.callback.xroot

val Handling.publishFromRoot: Handling get() =
    resolve<Context>()?.run { xroot.publish } ?: this