package me.simpleHook.feature.config.ui

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.ScrollView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.core.base.BaseBottomViewFragment
import me.simpleHook.data.HookConfig
import me.simpleHook.data.local.db.entity.CollectionEntity
import me.simpleHook.core.extension.showPopup
import me.simpleHook.feature.config.ui.delegate.CollectionEnviDelegate
import me.simpleHook.feature.config.ui.delegate.CollectionItemViewDelegate
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.feature.config.ui.view.CollectionFragmentView
import me.simpleHook.feature.config.ui.view.InputCollectionView
import me.simpleHook.feature.config.viewmodel.CollectionViewModel
import java.util.regex.Pattern

class CollectionViewFragment(private val addConfig: (CollectionEntity) -> Unit) :
    BaseBottomViewFragment<CollectionFragmentView>() {
    private val collectionViewModel by viewModels<CollectionViewModel>()
    private val multiAdapter = MultiTypeAdapter()
    override fun initRootView(): CollectionFragmentView {
        return CollectionFragmentView(requireContext())
    }

    private val json by lazy {
        Json {
            prettyPrint = true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun init() {
        multiAdapter.register(CollectionItemViewDelegate(onClick = {
            if (it.config.contains("""\$\{(.*)\}""".toRegex())) {
                showImportCollectDialog(it)
            } else {
                addConfig(it)
            }
        }, onLongClick = {
            showEditCollectConfigDialog(it)
        }))
        root.listView.adapter = multiAdapter
        collectionViewModel.getAllCollections().observe(viewLifecycleOwner) {
            multiAdapter.items = it
            multiAdapter.notifyDataSetChanged()
            root.progressBar.hide()
        }
    }

    private fun showImportCollectDialog(collectionEntity: CollectionEntity) {
        val matcher = Pattern.compile("""\$\{(.*?)\}""").matcher(collectionEntity.config)
        val list = ArrayList<String>()
        while (matcher.find()) {
            matcher.group(1)?.let { list.add(it) }
        }
        val hashMap = HashMap<String, String>()
        val multiTypeAdapter = MultiTypeAdapter(list)
        multiTypeAdapter.register(CollectionEnviDelegate { key, value ->
            hashMap[key] = value
        })
        val recyclerView = RecyclerView(requireContext()).apply {
            adapter = multiTypeAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val dialog = customDialog(requireContext(),
            getString(R.string.config_collection_edit_var),
            contentView = recyclerView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                var tempConfig = collectionEntity.config
                hashMap.forEach {
                    tempConfig = tempConfig.replaceFirst("\${${it.key}}", it.value)
                }
                addConfig(collectionEntity.copy(config = tempConfig))
            },
            cancelText = getString(R.string.dialog_cancel),
            cancelAble = false)
        dialog.show()
        val window = dialog.window
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    private fun showEditCollectConfigDialog(collectionEntity: CollectionEntity) {
        val inputCollectionView = InputCollectionView(requireContext()).apply {
            val config = runCatching {
                json.decodeFromString<HookConfig>(collectionEntity.config)
            }.getOrNull()
            config ?: requireActivity().showPopup(message = getString(R.string.common_error))
            configEditText.setText(json.encodeToString(config))
            nameEditText.setText(collectionEntity.name)
            configEditText.setOnFocusChangeListener { _, hasFocus ->
                insertEnviVar.isEnabled = hasFocus
            }
        }
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(inputCollectionView)
        }
        customDialog(requireContext(),
            title = getString(R.string.config_collection_edit_collection),
            contentView = scrollView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                val name = inputCollectionView.nameEditText.text.toString()
                val config: String? = runCatching {
                    val hookConfig =
                        Json.decodeFromString<HookConfig>(inputCollectionView.configEditText.text.toString())
                    Json.encodeToString(hookConfig)
                }.getOrNull()
                config?.let {
                    collectionViewModel.updateCollections(collectionEntity.copy(name = name,
                        config = it))
                }
                    ?: requireActivity().showPopup(message = getString(R.string.config_collection_illegal_format))

            },
            cancelText = getString(R.string.dialog_cancel),
            neutralText = getString(R.string.delete),
            neutralClick = {
                collectionViewModel.deleteCollections(collectionEntity)
            }).show()
    }
}
