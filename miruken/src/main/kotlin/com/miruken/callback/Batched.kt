package com.miruken.callback

data class Batched<T>(val callback: T, val rawCallback: Any)