package com.miruken.callback.policy.rules

import com.miruken.callback.policy.rules.ReturnRule

open class ReturnRuleDelegate(val rule: ReturnRule) : ReturnRule by rule