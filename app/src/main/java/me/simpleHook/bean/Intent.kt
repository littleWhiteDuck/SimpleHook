package me.simpleHook.bean

data class IntentBean(val packageName: String, val className: String,
                      val action: String, val data: String,
                      val extras: List<ExtraBean>)
data class ExtraBean(val type: String, val key: String, val value: String)