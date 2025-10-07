package me.simpleHook.ui.activity

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.drakeet.about.AbsAboutActivity
import com.drakeet.about.Card
import com.drakeet.about.Category
import com.drakeet.about.Contributor
import com.drakeet.about.License
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.util.AssetsUtil


class AboutActivity : AbsAboutActivity() {


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.drawable.ic_launcher_foreground)
        slogan.text = BuildConfig.APP_NAME
        version.text = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("Introduction"))
        items.add(Card("https://github.com/littleWhiteDuck/SimpleHook"))

        items.add(Category("Developers"))
        items.add(Contributor(R.drawable.author, "littleWhiteDuck", "Developer & designer"))
        items.add(Contributor(R.drawable.xiaoniu, "快乐小牛", "Developer & tester"))

        /* items.add(
             Contributor(
                 R.drawable.ic_github,
                 "Source Code (Hook)",
                 "https://github.com/littleWhiteDuck/SimpleHook",
                 "https://github.com/littleWhiteDuck/SimpleHook"
             )
         )*/

        items.add(Category("Testers"))
        items.add(Contributor(R.drawable.jian, "简大仙", "Tester and Sponsor"))
        items.add(Contributor(R.drawable.zhengji, "正己", "Tester"))

        items.add(Category("App recommendations"))
        items.add(
            Contributor(
                R.drawable.jshook,
                "JsHook",
                getString(R.string.about_js_hook),
                "https://github.com/Xposed-Modules-Repo/me.jsonet.jshook"
            )
        )


        items.add(Category("Contributions"))
        items.add(
            Card(
                """
                反馈BUG较多: @Hidarihitomi
            """.trimIndent()
            )
        )

        items.add(Category("Open Source Licenses"))

        val libStr = AssetsUtil.getText(this, "lib_license.json")!!
        val licenses = Json.decodeFromString<List<License2>>(libStr).map {
            License(it.name, it.author, it.type, it.url)
        }
        items.addAll(licenses)

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

@Serializable
data class License2(
    val name: String,
    val author: String,
    val type: String,
    val url: String
)