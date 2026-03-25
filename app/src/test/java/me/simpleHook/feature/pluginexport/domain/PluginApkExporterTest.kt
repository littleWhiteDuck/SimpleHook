package me.simpleHook.feature.pluginexport.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PluginApkExporterTest {

    @Test
    fun parseVersionCode_acceptsPositiveInteger() {
        assertEquals(1, PluginApkExporter.parseVersionCode("1"))
        assertEquals(1024, PluginApkExporter.parseVersionCode(" 1024 "))
    }

    @Test
    fun parseVersionCode_rejectsInvalidValue() {
        assertNull(PluginApkExporter.parseVersionCode(""))
        assertNull(PluginApkExporter.parseVersionCode("0"))
        assertNull(PluginApkExporter.parseVersionCode("-1"))
        assertNull(PluginApkExporter.parseVersionCode("1.0"))
        assertNull(PluginApkExporter.parseVersionCode("abc"))
    }
}
