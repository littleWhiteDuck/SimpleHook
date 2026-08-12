package me.simpleHook.core.extension

fun <T> Map<String, T>.get(key: String, default: T): T {
    return get(key) ?: default
}