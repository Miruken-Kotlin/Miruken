package com.miruken.mvc.policy

interface PolicyOwner<out P: Policy> {
    val policy: P
}