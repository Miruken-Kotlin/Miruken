package com.miruken.callback

import com.miruken.callback.policy.*
import com.miruken.getMethod
import com.miruken.runtime.getTaggedAnnotations
import org.junit.Test
import kotlin.test.assertTrue

class ProvidesPolicyTest {
    class Foo

    @Suppress("UNUSED_PARAMETER")
    class MyProvider {
        @Provides
        fun provideFoo(): Foo { return Foo() }
    }

    @Test
    fun `Gets ProvidesPolicy from @Provides annotation`() {
        val member   = getMethod<MyProvider>("provideFoo")
        val provides = member!!.getTaggedAnnotations<UsePolicy<*>>().first()
        assertTrue(provides.second.single().policy == ProvidesPolicy)
    }
}
