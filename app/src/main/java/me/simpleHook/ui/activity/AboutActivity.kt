package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import littleWhiteDuck.WindowPreferencesManager
import me.simpleHook.R
import me.simpleHook.adapter.BasicViewHolder
import me.simpleHook.adapter.BasicViewHolderFactory
import me.simpleHook.adapter.MultiTypeAdapter
import me.simpleHook.bean.Author
import me.simpleHook.bean.OpenSource
import me.simpleHook.bean.Title
import me.simpleHook.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    private val itemList = ArrayList<Any>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val windowPreferencesManager = WindowPreferencesManager(this)
        windowPreferencesManager.applyEdgeToEdgePreference(window)
        initData()
        initView()
    }

    private fun initData() {
        itemList.apply {
            add(Title("开发者"))
            add(
                Author(
                    "littleWhiteDuck",
                    "CV Engineering major of Home University",
                    R.drawable.author
                )
            )
            add(Title("测试人员"))
            add(Author("正己", "测试bug", null))
            add(Author("简", "测试bug", null))
            add(Title("开源相关"))
            add(
                OpenSource(
                    "kotlin - JetBrains",
                    "https://github.com/JetBrains/Kotlin",
                    "Apache software license"
                )
            )
            add(
                OpenSource(
                    "AndroidX - Google",
                    "https://source.google.com",
                    "Apache software license 2.0"
                )
            )
            add(
                OpenSource(
                    "Android Jetpack - Google",
                    "https://source.google.com",
                    "Apache software license 2.0"
                )
            )
            add(
                OpenSource(
                    "material-components-android - Google",
                    "https://github.com/material-components/material-components-android",
                    "Apache software license 2.0"
                )
            )
            add(
                OpenSource(
                    "FloatingActionButton - Clans",
                    "https://github.com/Clans/FloatingActionButton",
                    "Apache software license 2.0"
                )
            )
            add(
                OpenSource(
                    "EasyFloat - princekin-f",
                    "https://github.com/princekin-f/EasyFloat",
                    "Apache software license 2.0"
                )
            )
            add(
                OpenSource(
                    "gson - Google",
                    "https://github.com/google/gson",
                    "Apache software license 2.0"
                )
            )
            add(Title("其他"))
            add(OpenSource("XposedTinker - w296488320(珍惜)",
            "https://github.com/w296488320/XposedTinker", "https://bbs.pediy.com/thread-255700.com"))
        }
    }

    private fun initView() {
        binding.apply {
            rev.adapter = MultiTypeAdapter(itemList, object : BasicViewHolderFactory() {
                override fun getLayoutResId(position: Int, data: Any): Int {
                    return when (data) {
                        is Title -> R.layout.item_about_title
                        is OpenSource -> R.layout.item_about_open_source
                        is Author -> R.layout.item_about_author
                        else -> throw IllegalArgumentException("unknown data: $data")
                    }
                }

                override fun onCreateViewHolder(
                    inflater: LayoutInflater,
                    parent: ViewGroup,
                    layoutResId: Int
                ): BasicViewHolder<*> {
                    val itemView = inflater.inflate(layoutResId, parent, false)
                    return when (layoutResId) {
                        R.layout.item_about_title -> TitleHolder(itemView)
                        R.layout.item_about_open_source -> OpenSourceHolder(itemView)
                        R.layout.item_about_author -> AuthorHolder(itemView)
                        else -> throw IllegalArgumentException("unknown layout: $layoutResId")
                    }
                }

            })
            rev.layoutManager = LinearLayoutManager(this@AboutActivity)
            rev.addItemDecoration(
                DividerItemDecoration(
                    this@AboutActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }

    }

    class AuthorHolder(itemView: View) : BasicViewHolder<Author>(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.name)
        private val tvIntro = itemView.findViewById<TextView>(R.id.introduce)
        private val ivIcon = itemView.findViewById<ImageView>(R.id.imageView)
        override fun onBindData(position: Int, data: Author) {
            tvName.text = data.name
            tvIntro.text = data.introduce
            data.id?.let { ivIcon.setImageResource(it) }
        }

    }

    class OpenSourceHolder(itemView: View) : BasicViewHolder<OpenSource>(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.name)
        private val tvOpen = itemView.findViewById<TextView>(R.id.openSource)
        private val intent = Intent(Intent.ACTION_VIEW)
        override fun onBindData(position: Int, data: OpenSource) {
            tvName.text = data.name
            val str = data.link + "\n" + data.License
            tvOpen.text = str
            itemView.setOnClickListener {
                intent.data = Uri.parse(data.link)
                itemView.context.startActivity(intent)
            }
        }

    }

    class TitleHolder(itemView: View) : BasicViewHolder<Title>(itemView) {
        private val tvTitle = itemView as TextView
        override fun onBindData(position: Int, data: Title) {
            tvTitle.text = data.title
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }
}