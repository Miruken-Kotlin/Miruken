package com.miruken.validate.bean

import com.miruken.callback.Handling
import javax.validation.ValidatorContext

interface CascadeAware {
    fun setCascadeInfo(
            validatorContext: ValidatorContext,
            composer:         Handling
    )
}