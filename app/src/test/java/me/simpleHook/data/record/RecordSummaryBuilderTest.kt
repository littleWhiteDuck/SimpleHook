package me.simpleHook.data.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecordSummaryBuilderTest {

    @Test
    fun build_paramRecord_usesMethodSignatureOnly() {
        val summary = RecordSummaryBuilder.build(
            record = RecordParam(
                className = "com.demo.LoginService",
                methodName = "checkPassword",
                params = listOf("java.lang.String", "byte[]"),
                paramValues = listOf(mapOf(RecordValueType.ToString to SECRET_VALUE))
            ),
            type = RecordType.RecordParam
        )

        assertEquals("LoginService.checkPassword", summary.title)
        assertEquals("com.demo.LoginService#checkPassword(java.lang.String, byte[])", summary.subtitle)
        assertEquals(RecordSummaryBuilder.META_PARAM, summary.meta)
        assertDoesNotLeakValue(summary)
    }

    @Test
    fun build_returnRecord_ignoresReturnValue() {
        val summary = RecordSummaryBuilder.build(
            record = RecordReturn(
                className = "com.demo.LoginService",
                methodName = "token",
                params = emptyList(),
                returnValue = mapOf(RecordValueType.ToString to SECRET_VALUE)
            ),
            type = RecordType.RecordReturn
        )

        assertEquals("LoginService.token", summary.title)
        assertEquals("com.demo.LoginService#token()", summary.subtitle)
        assertEquals(RecordSummaryBuilder.META_RETURN, summary.meta)
        assertDoesNotLeakValue(summary)
    }

    @Test
    fun build_paramReturnRecord_ignoresParamAndReturnValues() {
        val summary = RecordSummaryBuilder.build(
            record = RecordParamReturn(
                className = "com.demo.LoginService",
                methodName = "login",
                params = listOf("java.lang.String"),
                paramValues = listOf(mapOf(RecordValueType.ToString to SECRET_VALUE)),
                returnValue = mapOf(RecordValueType.ToString to SECRET_VALUE)
            ),
            type = RecordType.RecordParamReturn
        )

        assertEquals("LoginService.login", summary.title)
        assertEquals("com.demo.LoginService#login(java.lang.String)", summary.subtitle)
        assertEquals(RecordSummaryBuilder.META_PARAM_RETURN, summary.meta)
        assertDoesNotLeakValue(summary)
    }

    @Test
    fun build_staticFieldRecord_usesFieldOwnerAndName() {
        val summary = RecordSummaryBuilder.build(
            record = RecordField(
                fieldClassName = "com.demo.AuthManager",
                fieldName = "tokenCache",
                filedValue = mapOf(RecordValueType.ToString to SECRET_VALUE)
            ),
            type = RecordType.RecordField
        )

        assertEquals("tokenCache", summary.title)
        assertEquals("com.demo.AuthManager", summary.subtitle)
        assertEquals(RecordSummaryBuilder.META_STATIC_FIELD, summary.meta)
        assertDoesNotLeakValue(summary)
    }

    @Test
    fun build_instanceFieldRecord_usesCallSignatureAndFieldName() {
        val summary = RecordSummaryBuilder.build(
            record = RecordField(
                className = "com.demo.AuthManager",
                methodName = "login",
                params = listOf("java.lang.String"),
                fieldName = "session",
                filedValue = mapOf(RecordValueType.ToString to SECRET_VALUE)
            ),
            type = RecordType.RecordField
        )

        assertEquals("session", summary.title)
        assertEquals("com.demo.AuthManager#login(java.lang.String)", summary.subtitle)
        assertEquals(RecordSummaryBuilder.META_INSTANCE_FIELD, summary.meta)
        assertDoesNotLeakValue(summary)
    }

    private fun assertDoesNotLeakValue(summary: RecordListSummary) {
        assertFalse(summary.title.orEmpty().contains(SECRET_VALUE))
        assertFalse(summary.subtitle.orEmpty().contains(SECRET_VALUE))
        assertFalse(summary.meta.orEmpty().contains(SECRET_VALUE))
    }

    private companion object {
        const val SECRET_VALUE = "SECRET_REAL_VALUE"
    }
}
