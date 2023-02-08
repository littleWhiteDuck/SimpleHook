package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.drakeet.about.*
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.util.ToolUtils
import me.simpleHook.util.toast


class AboutActivity : AbsAboutActivity() {


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.feedback -> {
                ToolUtils.toClip(this, "484303285@qq.com")
                getString(R.string.about_clip_email_tip).toast(this)
            }
            R.id.add_group -> {
                val intent = Intent(ACTION_VIEW).also {
                    it.data = Uri.parse("https://t.me/simpleHook")
                }
                startActivity(intent)
            }
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
//        icon.setImageResource(R.drawable.ic_launcher)
        slogan.text = BuildConfig.APP_NAME
        version.text = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("介绍与帮助"))
        items.add(Card("https://github.com/littleWhiteDuck/SimpleHook"))

        items.add(Category("Developers"))
        items.add(Contributor(R.drawable.author, "littleWhiteDuck", "Developer & designer"))
        items.add(
            Contributor(
                R.drawable.ic_github,
                "Source Code (Hook)",
                "https://github.com/littleWhiteDuck/SimpleHook",
                "https://github.com/littleWhiteDuck/SimpleHook"
            )
        )

        items.add(Category("Testers"))
        items.add(Contributor(R.drawable.zhengji, "正己", "Tester"))
        items.add(Contributor(R.drawable.jian, "简", "Tester"))
        items.add(Contributor(R.drawable.xiaoniu, "快乐小牛", "Tester"))

        items.add(Category("Open Source Licenses"))
        items.add(
            License(
                "Kotlin", "JetBrains", License.APACHE_2, "https://github.com/JetBrains/Kotlin"
            )
        )
        items.add(License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"))
        items.add(
            License(
                "Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"
            )
        )
        items.add(
            License(
                "material-components-android",
                "Google",
                License.APACHE_2,
                "https://github.com/material-components/material-components-android"
            )
        )
        items.add(
            License(
                "EasyFloat",
                "princekin-f",
                License.APACHE_2,
                "https://github.com/princekin-f/EasyFloat"
            )
        )
        items.add(License("gson", "Google", License.APACHE_2, "https://github.com/google/gson"))
        items.add(
            License(
                "XposedBridge", "rovo89", License.APACHE_2, "https://github.com/rovo89/XposedBridge"
            )
        )
        items.add(
            License(
                "libsu", "topjohnwu", License.APACHE_2, "https://github.com/topjohnwu/libsu"
            )
        )
        items.add(
            License(
                "Glide",
                "Sam Judd",
                "BSD, part MIT and Apache 2.0",
                "https://github.com/bumptech/glide"
            )
        )
        items.add(
            License(
                "SwipeDelMenuLayout",
                "mcxtzhang",
                License.APACHE_2,
                "https://github.com/mcxtzhang/SwipeDelMenuLayout"
            )
        )
        items.add(
            License(
                "EzXHelper", "KyuubiRan", License.APACHE_2, "https://github.com/KyuubiRan/EzXHelper"
            )
        )
        items.add(
            License(
                "SimpleMenuPreference",
                "RikkaX",
                License.MIT,
                "https://github.com/RikkaApps/RikkaX/tree/master/preference/simplemenu-preference"
            )
        )
        items.add(
            License(
                "MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"
            )
        )
        items.add(
            License(
                "about-page",
                "drakeet",
                License.APACHE_2,
                "https://github.com/PureWriter/about-page"
            )
        )
        items.add(
            License(
                "Kotlin multiplatform / multi-format reflectionless serialization",
                "JetBrains",
                License.APACHE_2,
                "https://github.com/Kotlin/kotlinx.serialization"
            )
        )


        items.add(Category("Acknowledgement"))
        items.add(
            Card(
                """
            应用中部分图标来源于：
            https://www.iconfont.cn/
        """.trimIndent()
            )
        )
    }

}