package me.simpleHook.platform.hook.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HookTypeParserTest {

    @Test
    fun getClassTypeName_handlesPrimitiveArray() {
        assertEquals("[B", HookTypeParser.getClassTypeName("byte[]"))
        assertEquals("[[Z", HookTypeParser.getClassTypeName("boolean[][]"))
    }

    @Test
    fun getClassTypeName_handlesObjectArray() {
        assertEquals("[Ljava.lang.String;", HookTypeParser.getClassTypeName("java.lang.String[]"))
        assertEquals("[Landroid.content.Context;", HookTypeParser.getClassTypeName("Context[]"))
    }
}
