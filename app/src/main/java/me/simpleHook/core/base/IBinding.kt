package me.simpleHook.core.base

import androidx.viewbinding.ViewBinding

internal sealed interface IBinding<VB : ViewBinding> {
    val binding: VB
}