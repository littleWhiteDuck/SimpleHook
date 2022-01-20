package me.simpleHook.ui.activity

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.R
import me.simpleHook.adapter.BasicViewHolder
import me.simpleHook.adapter.BasicViewHolderFactory
import me.simpleHook.adapter.MultiTypeAdapter
import me.simpleHook.bean.Author
import me.simpleHook.bean.OpenSource
import me.simpleHook.bean.Title
import me.simpleHook.databinding.ActivityAboutBinding
import me.simpleHook.ui.view.about.AuthorView
import me.simpleHook.ui.view.about.OpenSourceView
import me.simpleHook.util.dp

class AboutActivity : BaseActivity() {
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
            add(Title(getString(R.string.about_title_author)))
            add(
                Author(
                    getString(R.string.about_name_developer),
                    getString(R.string.about_introduce_developer),
                    R.drawable.author
                )
            )
            add(Title(getString(R.string.about_title_tester)))
            add(Author(getString(R.string.about_tester_zj), getString(R.string.about_introduce_test_bug), R.drawable.zhengji))
            add(Author(getString(R.string.about_tester_j), getString(R.string.about_introduce_test_bug), R.drawable.jian))
            add(Title(getString(R.string.about_title_open_sources)))
            add(
                OpenSource(
                    "kotlin - JetBrains",
                    "https://github.com/JetBrains/Kotlin",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "AndroidX - Google",
                    "https://source.google.com",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "Android Jetpack - Google",
                    "https://source.google.com",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "material-components-android - Google",
                    "https://github.com/material-components/material-components-android",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "FloatingActionButton - Clans",
                    "https://github.com/Clans/FloatingActionButton",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "EasyFloat - princekin-f",
                    "https://github.com/princekin-f/EasyFloat",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "gson - Google",
                    "https://github.com/google/gson",
                    getString(R.string.about_source_license_2)
                )
            )
            add(
                OpenSource(
                    "XposedBridge - rovo89",
                    "https://github.com/rovo89/XposedBridge",
                    ""
                )
            )
        }
    }

    private fun initView() {
        binding.apply {
            rev.adapter = MultiTypeAdapter(itemList, object : BasicViewHolderFactory() {
                override fun getItemViewType(position: Int, data: Any): Int {
                    return when (data) {
                        is Title -> 1
                        is OpenSource -> 2
                        is Author -> 3
                        else -> throw IllegalArgumentException("unknown data: $data")
                    }
                }

                override fun getItemView(parent: ViewGroup, viewType: Int) = when (viewType) {
                    1 -> AppCompatTextView(parent.context).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.setMargins(12.dp, 0, 0, 0)
                        }
                        setPadding(0, 5.dp, 0, 5.dp)
                    }
                    2 -> OpenSourceView(parent.context)
                    3 -> AuthorView(parent.context)
                    else -> throw IllegalArgumentException("unknown viewType: $viewType")
                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    itemView: View
                ): BasicViewHolder<*> {

                    return when (itemView) {
                        is AppCompatTextView -> TitleHolder(itemView)
                        is OpenSourceView -> OpenSourceHolder(itemView)
                        is AuthorView -> AuthorHolder(itemView)
                        else -> throw IllegalArgumentException("unknown itemView: $itemView")
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
            rev.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        return
                    }

                    if (position == parent.adapter!!.itemCount - 1) {

                        outRect.bottom = 200
                    }
                }
            })

        }

    }

    class AuthorHolder(itemView: View) : BasicViewHolder<Author>(itemView) {
        private val authorView = itemView as AuthorView
        private val tvName = authorView.name
        private val tvIntro = authorView.introduce
        private val ivIcon = authorView.icon
        override fun onBindData(position: Int, data: Author) {
            tvName.text = data.name
            tvIntro.text = data.introduce
            data.id?.let { ivIcon.setImageResource(it) }
        }

    }

    class OpenSourceHolder(itemView: View) : BasicViewHolder<OpenSource>(itemView) {
        private val openSourceView = itemView as OpenSourceView
        private val tvName = openSourceView.name
        private val tvOpen = openSourceView.openSource
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
        private val tvTitle = itemView as AppCompatTextView
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