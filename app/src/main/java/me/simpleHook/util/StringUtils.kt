package me.simpleHook.util

import android.content.Context
import android.widget.Toast
import de.robv.android.xposed.XposedBridge

//toast
fun String.toast(context: Context,duration: Int = Toast.LENGTH_SHORT){
    Toast.makeText(context,this,duration).show()
}

//xposed log
fun String.log(){
    XposedBridge.log("===${this}===")
}