package me.simpleHook.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordSummary(val type: String = "", val packageName: String = "", val count: Int = 0) :
    Parcelable
