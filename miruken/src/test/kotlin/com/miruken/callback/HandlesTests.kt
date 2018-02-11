package com.miruken.callback

import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertTrue
import org.junit.Test as test

class HandlesTests {
    @test fun `Handles is a Category`() {
        val handles = Handles::class
        assertTrue { handles.isSubclassOf(Annotation::class) }
     }
}
