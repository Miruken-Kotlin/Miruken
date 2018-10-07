package com.miruken.callback.policy.rules

import com.miruken.callback.policy.rules.ArgumentRule

open class ArgumentRuleDelegate(val rule: ArgumentRule) : ArgumentRule by rule