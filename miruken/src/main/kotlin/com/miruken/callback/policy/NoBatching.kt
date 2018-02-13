package com.miruken.callback.policy

import com.miruken.callback.BatchingCallback
import com.miruken.callback.Trampoline

class NoBatching(callback: Any)
    : Trampoline(callback), BatchingCallback {

    override val allowBatching = false
}