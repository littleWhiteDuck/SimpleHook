package littleWhiteDuck

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object Utils {
    @SuppressLint("WrongConstant")
    fun commitClip(context: Context, msg: String){
        (context.getSystemService("clipboard") as ClipboardManager).setPrimaryClip(ClipData.newPlainText("littleWhiteDuck", msg))
        Toast.makeText(context,"邮箱已复制到剪切板",Toast.LENGTH_LONG).show()
    }
}