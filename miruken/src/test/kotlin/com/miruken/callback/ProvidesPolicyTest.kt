package com.miruken.callback

import com.miruken.callback.policy.UsePolicy
import com.miruken.callback.policy.policy
import com.miruken.getMethod
import com.miruken.runtime.getMetaAnnotations
import org.junit.Test
import kotlin.test.assertEquals

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
        val provides = member!!.getMetaAnnotations<UsePolicy>().first()
        assertEquals(provides.second.single().policy, ProvidesPolicy)
    }
}
