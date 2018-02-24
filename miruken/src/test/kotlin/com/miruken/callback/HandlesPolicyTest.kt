package com.miruken.callback

import com.miruken.callback.policy.*
import com.miruken.getMethod
import com.miruken.runtime.*
import org.junit.*
import kotlin.test.assertTrue

class HandlesPolicyTest {
    class Foo

    @Suppress("UNUSED_PARAMETER")
    class MyHandler{
        @Handles
        fun handleFoo(foo: Foo) {}
    }

    @Test fun `Gets HandlesPolicy from @Handles annotation`() {
        val member  = getMethod<MyHandler>("handleFoo")
        val handles = member!!.getTaggedAnnotations<UsePolicy<*>>().first()
        assertTrue(handles.second.single().policy == HandlesPolicy)
    }
}
