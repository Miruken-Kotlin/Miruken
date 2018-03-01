package com.miruken.callback

class RejectedException(val callback: Any)
    : RuntimeException("Callback '$callback' has been rejected")