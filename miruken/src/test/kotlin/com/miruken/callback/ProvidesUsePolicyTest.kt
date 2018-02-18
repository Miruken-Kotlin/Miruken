package com.miruken.callback

import com.miruken.callback.policy.*
import com.miruken.getMethod
import com.miruken.RuntimeHelpers.getTaggedAnnotations
import org.junit.Test

class ProvidesUsePolicyTest {
    class Foo

    @Suppress("UNUSED_PARAMETER")
    class MyProvider {
        @Provides
        fun provideFoo() : Foo { return Foo() }
    }

    @Test
    fun `Gets ProvidesPolicy from @Provides annotation`() {
        val member   = getMethod<MyProvider>("provideFoo")
        val provides = member!!.getTaggedAnnotations<UsePolicy<*>>().first()
        kotlin.test.assertTrue { provides.policy == ProvidesPolicy }
    }
}
