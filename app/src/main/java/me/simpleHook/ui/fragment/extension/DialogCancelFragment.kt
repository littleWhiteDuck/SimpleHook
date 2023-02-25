package me.simpleHook.ui.fragment.extension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.chip.Chip
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.base.BasePreferenceFragment
import me.simpleHook.bean.DialogCancel
import me.simpleHook.databinding.LayoutInputKeywordBinding
import me.simpleHook.extension.showToast
import me.simpleHook.ui.custom.ChipPreference
import me.simpleHook.ui.custom.customDialog
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.ui.view.edit.InputView
import me.simpleHook.viewmodel.ExViewModel

class DialogCancelFragment : BasePreferenceFragment() {
    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
    }

    private lateinit var tempConfig: DialogCancel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val dialogCancelInfo = exViewModel.extensionConfig.value?.stopDialog?.info
            ?: throw NullPointerException("DialogCancel is null...")
        tempConfig = if (dialogCancelInfo.startsWith("{")) {
            Json.decodeFromString(dialogCancelInfo)
        } else {
            val list = dialogCancelInfo.split("\n")
            DialogCancel(keywords = Json.encodeToString(list))
        }
        val keywordSwitch = SwitchPreferenceCompat(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_dialog_cancel_title_keyword)
            isIconSpaceReserved = false
            summary = getString(R.string.extension_dialog_cancel_summary_keyword)
            isChecked = tempConfig.keywordEnable
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.keywordEnable = newValue as Boolean
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
            chipTexts = Json.decodeFromString(tempConfig.keywords)
        }
        val idSwitch = SwitchPreferenceCompat(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_dialog_cancel_title_id)
            isIconSpaceReserved = false
            isChecked = tempConfig.idEnable
            summary = getString(R.string.extension_dialog_cancel_summary_id)
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.idEnable = newValue as Boolean
                true
            }
        }
        val addIds = ImagePreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_dialog_cancel_title_add_id)
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
                showAddIdDialog()
                true
            }
        }
        val idChipPreference = ChipPreference(requireContext()) { showAddIdDialog(it) }.apply {
            isPersistent = false
            isIconSpaceReserved = false
            layoutResource = R.layout.layout_chip_group
            key = KEY_CHIP_GROUP_ID
            chipTexts = Json.decodeFromString(tempConfig.ids)
        }

        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.label_dialog)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.apply {
            addPreference(keywordSwitch)
            addPreference(addKeyWords)
            addPreference(chipPreference)
            addPreference(idSwitch)
            addPreference(addIds)
            addPreference(idChipPreference)
        }
        setPreferenceScreen(preferenceScreen)
    }

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
                    ?: findPreference<ChipPreference>(KEY_CHIP_GROUP)?.addChip(inputView.keywordEdit.text.toString()
                        .trim())
            },
            cancelAble = false,
            cancelText = getString(R.string.dialog_cancel),
            neutralText = if (chip != null) getString(R.string.config_dialog_delete) else "",
            neutralClick = {
                chip?.let { chipGroup!!.removeView(chip) }
            }).show()
    }


    @SuppressLint("ResourceAsColor")
    private fun showAddIdDialog(chip: Chip? = null) {
        val chipGroup = findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipGroup
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint =
                context.getString(R.string.extension_dialog_cancel_hint_dec_or_hex)
        }
        chip?.let { inputView.editText.setText(chip.text) }
        customDialog(requireContext(),
            title = getString(R.string.extension_dialog_cancel_add_id),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                val text = inputView.editText.text.toString().trim()
                if (text.isEmpty()) requireActivity().showToast(getString(R.string.extension_dialog_cancel_id_is_empty))
                val id = formatId(text)
                id?.let { str ->
                    chip?.let {
                        chip.text = str
                    } ?: findPreference<ChipPreference>(KEY_CHIP_GROUP_ID)?.addChip(str)
                }
                    ?: requireActivity().showToast(getString(R.string.extension_dialog_cancel_illegal_format))
            },
            cancelAble = false,
            cancelText = getString(R.string.dialog_cancel),
            neutralText = if (chip != null) getString(R.string.config_dialog_delete) else "",
            neutralClick = {
                chip?.let { chipGroup!!.removeView(chip) }
            }).show()
    }

    override fun init() {
        setDividerHeight(0)
        initMenu()
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)

        })
    }

    override fun canBack(): Boolean {
        return Json.encodeToString(tempConfig) == exViewModel.extensionConfig.value!!.stopDialog.info
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

    private fun saveConfig(exit: Boolean) {
        val keywordChipGroup =
            findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipGroup ?: throw NullPointerException(
                "chip is null")
        val idChipGroup = findPreference<ChipPreference>(KEY_CHIP_GROUP_ID)?.chipGroup
            ?: throw NullPointerException("chip is null")
        val keywords = ArrayList<String>()
        keywordChipGroup.children.forEach {
            keywords.add((it as Chip).text.toString())
        }
        tempConfig.keywords = Json.encodeToString(keywords)
        val ids = ArrayList<String>()
        idChipGroup.children.forEach {
            ids.add((it as Chip).text.toString())
        }
        tempConfig.ids = Json.encodeToString(ids)
        exViewModel.extensionConfig.value!!.stopDialog.info = Json.encodeToString(tempConfig)
        if (exit) navController.navigateUp()
    }

    private fun formatId(text: String): String? {
        return runCatching {
            if (text.contains(Regex("""[A-z]"""))) {
                text.replace("0x", "", true).toInt(16).toString()
            } else {
                text.toInt().toString()
            }
        }.getOrNull()
    }

    companion object {
        private const val KEY_CHIP_GROUP = "CHIP_GROUP_KEYWORD"
        private const val KEY_CHIP_GROUP_ID = "CHIP_GROUP_ID"
    }
}