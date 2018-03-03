package com.miruken.callback.policy

open class BivariantPolicy internal constructor() : CallbackPolicy() {

    lateinit var output: CovariantPolicy
        internal set

    lateinit var input:  ContravariantPolicy
        internal set

    constructor(
            build: BivariantKeyBuilder.() -> CallbackPolicyBuilder.Completed
    ) : this() {
        @Suppress("LeakingThis")
        val builder = BivariantKeyBuilder(this)
        builder.build()
    }

    override fun getKey(callback: Any): Any? =
            output.getKey(callback) to input.getKey(callback)

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any> {
        if (key !is Pair<*,*>) return emptyList()
        val (outputKey, inputKey) = key
        val compatible = mutableListOf<Pair<*,*>>()
        available.filterIsInstance<Pair<*,*>>().forEach {
            val (testOutKey, testInKey) = it
            val inEqual  = inputKey  == testInKey
            val outEqual = outputKey == testOutKey
            if (!outEqual && output.getCompatibleKeys(
                 outputKey!!, listOf(testOutKey!!)).isEmpty()) {
                return@forEach
            }
            if (!inEqual && input.getCompatibleKeys(
                 inputKey!!, listOf(testInKey!!)).isEmpty()) {
                return@forEach
            }
            compatible.add(it)
        }
        return compatible
    }

    override fun acceptResult(result: Any?, binding: PolicyMethodBinding) =
        output.acceptResult(result, binding)

    override fun compare(o1: Any?, o2: Any?): Int {
        val pair1 = o1 as? Pair<*,*>
        val pair2 = o2 as? Pair<*,*>
        val order = input.compare(pair1?.second, pair2?.second)
        return order.takeUnless { it == 0 }
            ?: output.compare(pair1?.first, pair2?.first)
    }
}