package com.miruken.callback

class RejectedException(val callback: Any)
    : Exception("Callback has been rejected")