package me.simpleHook.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class MemberInfo : Parcelable {

    @Parcelize
    data class MethodInfo(
        val className: String,
        val methodName: String,
        val parameters: List<String>,
        val returnType: String
    ) : MemberInfo()

    @Parcelize
    data class FieldInfo(
        val className: String,
        val fieldName: String,
        val fieldType: String,
        val isStatic: Boolean
    ) : MemberInfo()
}