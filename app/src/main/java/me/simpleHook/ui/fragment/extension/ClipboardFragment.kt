package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.chip.Chip
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.base.BasePreferenceFragment
import me.simpleHook.data.ClipboardConfig
import me.simpleHook.databinding.LayoutInputKeywordBinding
import me.simpleHook.ui.custom.ChipPreference
import me.simpleHook.ui.custom.MaterialSwitchPreference
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.viewmodel.ExViewModel

class ClipboardFragment : BasePreferenceFragment() {
    private lateinit var clipboardConfig: ClipboardConfig
    private var tempConfig = ""
    private lateinit var navController: NavController
    private val exViewModel by activityViewModels<ExViewModel>()

    @SuppressLint("ResourceAsColor")
    private fun showAddKeyWordDialog(chip: Chip? = null) {
        val chipGroup = findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipGroup
        val inputView = LayoutInputKeywordBinding.inflate(layoutInflater, null, false)
        chip?.let { inputView.keywordEdit.setText(chip.text) }
        inputView.testEdit.doAfterTextChanged {
            runCatching {
                if (it.toString().trim().isNotEmpty() && it.toString().trim()
                        .contains(Regex(inputView.keywordEdit.text.toString().trim()))
                ) {
                    inputView.testInput.isErrorEnabled = false
                } else {
                    inputView.testInput.isErrorEnabled = true
                    inputView.testInput.error = getString(R.string.extension_clip_not_match)
                }
            }.onFailure {
                inputView.keywordInput.isErrorEnabled = true
                inputView.keywordInput.error = getString(R.string.extension_clip_illegal_format)
            }.onSuccess {
                inputView.keywordInput.isErrorEnabled = false
            }
        }
        inputView.keywordEdit.doAfterTextChanged {
            runCatching {
                if (inputView.testEdit.text.toString().trim()
                        .isNotEmpty() && inputView.testEdit.text.toString().trim()
                        .contains(Regex(it.toString()))
                ) {
                    inputView.testInput.isErrorEnabled = false
                } else {
                    inputView.testInput.isErrorEnabled = true
                    inputView.testInput.error = getString(R.string.extension_clip_not_match)
                }
            }.onFailure {
                inputView.keywordInput.isErrorEnabled = true
                inputView.keywordInput.error = getString(R.string.extension_clip_illegal_format)
            }.onSuccess {
                inputView.keywordInput.isErrorEnabled = false
            }
        }
        customDialog(requireContext(),
            title = getString(R.string.extension_clip_add_keyword),
            contentView = inputView.root,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                chip?.let {
                    chip.text = inputView.keywordEdit.text.toString().trim()
                }
                    ?: findPreference<ChipPreference>(KEY_CHIP_GROUP)?.addChip(
                        inputView.keywordEdit.text.toString()
                            .trim()
                    )
            },
            cancelAble = false,
            cancelText = getString(R.string.dialog_cancel),
            neutralText = if (chip != null) getString(R.string.config_dialog_delete) else "",
            neutralClick = {
                chip?.let { chipGroup!!.removeView(chip) }
            }).show()
    }


    private fun saveConfig(exit: Boolean) {
        val chipGroup =
            findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipGroup ?: throw NullPointerException(
                "chip is null"
            )
        val items = ArrayList<String>()
        chipGroup.children.iterator().forEach {
            items.add((it as Chip).text.toString())
        }
        val result = Json.encodeToString(items)
        clipboardConfig.filter = result
        exViewModel.extensionConfig.value!!.filterClipboard.info =
            Json.encodeToString(clipboardConfig)
        if (exit) navController.navigateUp()
    }

    override fun init() {
        setDividerHeight(0)
        clipboardConfig =
            Json.decodeFromString(exViewModel.extensionConfig.value!!.filterClipboard.info)
        tempConfig = clipboardConfig.toString()
        findPreference<MaterialSwitchPreference>(KEY_RECORD_READ_WRITE)?.isChecked =
            clipboardConfig.record
        findPreference<MaterialSwitchPreference>(KEY_READ_CLIPBOARD)?.isChecked =
            clipboardConfig.read
        findPreference<MaterialSwitchPreference>(KEY_WRITE_CLIPBOARD)?.isChecked =
            clipboardConfig.write
        if (clipboardConfig.filter.isNotEmpty()) {
            val list = Json.decodeFromString<List<String>>(clipboardConfig.filter)
            findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipTexts = list
        }
        navController = findNavController()
        initMenu()
    }

    override fun canBack(): Boolean {
        return tempConfig == clipboardConfig.toString()
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_clipboard, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_save -> saveConfig(true)
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val recordClip = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_clip_title_record)
            isIconSpaceReserved = false
            summary = getString(R.string.extension_clip_summary_record)
            key = KEY_RECORD_READ_WRITE
            setOnPreferenceChangeListener { _, newValue ->
                clipboardConfig.record = newValue as Boolean
                true
            }
        }
        val blockGetClip = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_clip_prevent_read_clip)
            isIconSpaceReserved = false
            key = KEY_READ_CLIPBOARD
            setOnPreferenceChangeListener { _, newValue ->
                clipboardConfig.read = newValue as Boolean
                true
            }
        }
        val blockWriteClip = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_clip_prevent_write_clip)
            isIconSpaceReserved = false
            summary = getString(R.string.extension_clip_summary_prevent_write_clip)
            key = KEY_WRITE_CLIPBOARD
            setOnPreferenceChangeListener { _, newValue ->
                clipboardConfig.write = newValue as Boolean
                true
            }
        }
        val addKeyWords = ImagePreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_clip_title_add_keyword)
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
                showAddKeyWordDialog()
                true
            }
        }
        val chipPreference = ChipPreference(requireContext()) { showAddKeyWordDialog(it) }.apply {
            isPersistent = false
            isIconSpaceReserved = false
            layoutResource = R.layout.layout_chip_group
            key = KEY_CHIP_GROUP
        }
        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.extension_clipboard)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.apply {
            addPreference(recordClip)
            addPreference(blockGetClip)
            addPreference(blockWriteClip)
            addPreference(addKeyWords)
            addPreference(chipPreference)
        }
        setPreferenceScreen(preferenceScreen)
    }

    companion object {
        private const val KEY_READ_CLIPBOARD = "READ_CLIPBOARD"
        private const val KEY_WRITE_CLIPBOARD = "WRITE_CLIPBOARD"
        private const val KEY_RECORD_READ_WRITE = "RECORD_READ_WRITE"
        private const val KEY_CHIP_GROUP = "CHIP_GROUP"
    }
}


class ImagePreference(context: Context) : Preference(context) {
    init {
        widgetLayoutResource = R.layout.layout_image_view
    }
}