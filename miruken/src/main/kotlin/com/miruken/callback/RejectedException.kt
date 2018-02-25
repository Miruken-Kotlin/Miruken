package com.miruken.callback

class RejectedException(val callback: Any)
    : Exception("Callback '$callback' has been rejected")