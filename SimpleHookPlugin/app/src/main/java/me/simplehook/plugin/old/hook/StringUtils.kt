package me.simplehook.plugin.old.hook

import kotlin.math.roundToInt

val String.toByteValue
    get() = run {
        val value = try {
            this.toByte()
        } catch (e: java.lang.NumberFormatException) {
            if (this.startsWith("-")) {
                Byte.MIN_VALUE
            } else {
                Byte.MAX_VALUE
            }
        }
        value
    }

val String.toIntValue
    get() = run {
        val value = try {
            this.toInt()
        } catch (e: java.lang.NumberFormatException) {
            if (this.startsWith("-")) {
                Int.MIN_VALUE
            } else {
                Int.MAX_VALUE
            }
        }
        value
    }

val String.toShortValue
    get() = run {
        val value = try {
            this.toShort()
        } catch (e: java.lang.NumberFormatException) {
            if (this.startsWith("-")) {
                Short.MIN_VALUE
            } else {
                Short.MAX_VALUE
            }
        }
        value
    }

val String.toLongValue
    get() = run {
        val value = try {
            this.toLong()
        } catch (e: java.lang.NumberFormatException) {
            if (this.startsWith("-")) {
                Long.MIN_VALUE
            } else {
                Long.MAX_VALUE
            }
        }
        value
    }

fun String.random(length: Int): String {
    val temp = StringBuilder()
    for (i in 0 until length) {
        temp.append(this[(Math.random() * (this.length - 1)).roundToInt()])
    }
    return temp.toString()
}