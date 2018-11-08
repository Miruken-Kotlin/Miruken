package com.miruken.callback

import com.miruken.callback.policy.UsePolicy
import com.miruken.callback.policy.policy
import com.miruken.test.getMethod
import com.miruken.runtime.getMetaAnnotations
import org.junit.Test
import kotlin.test.assertEquals

class HandlesPolicyTest {
    class Foo

    @Suppress("UNUSED_PARAMETER")
    class MyHandler{
        @Handles
        fun handleFoo(foo: Foo) {}
    }

    @Test fun `Gets HandlesPolicy from @Handles annotation`() {
        val member  = getMethod<MyHandler>("handleFoo")
        val handles = member!!.getMetaAnnotations<UsePolicy>().first()
        assertEquals(handles.second.single().policy, HandlesPolicy)
    }
}
