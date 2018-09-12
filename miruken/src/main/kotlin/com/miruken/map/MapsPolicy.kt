package com.miruken.map

import com.miruken.callback.policy.BivariantPolicy
import com.miruken.callback.policy.PolicyMemberBinding
import com.miruken.callback.policy.UsePolicy
import com.miruken.runtime.getTaggedAnnotations
import kotlin.reflect.KAnnotatedElement

@Target(AnnotationTarget.FUNCTION)
@UsePolicy(MapsPolicy::class)
annotation class Maps

object MapsPolicy : BivariantPolicy({
    key { cb: Mapping -> cb.targetType } target { source } rules {
        matches (target) returns key
        matches (target, callback) returns key
        matches (callback) returns (key or unit)
    }
}) {
    override fun approve(
            callback: Any,
            binding:  PolicyMemberBinding
    ) = (callback as? Mapping)?.let { mapping ->
        val format = mapping.format ?: return true
            return binding.dispatcher.let {
                matches(format, it) || matches(format, it.owningClass)
            }
        } ?: false

    private fun matches(format: Any, sources: KAnnotatedElement) =
            sources.getTaggedAnnotations<UseFormatMatcher<*>>().any {
                val (annotation, matcher) = it
                matcher.any { match ->
                    match.formatMatcherClass.objectInstance
                            ?.matches(annotation, format) == true
                }
            }
}