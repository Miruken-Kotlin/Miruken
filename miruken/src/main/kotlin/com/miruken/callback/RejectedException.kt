package com.miruken.callback

import java.util.concurrent.CancellationException

class RejectedException(val callback: Any?)
    : CancellationException("Callback '$callback' has been rejected")