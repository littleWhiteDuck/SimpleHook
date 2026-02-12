package me.simpleHook.feature.extension.ui

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
import androidx.navigation.findNavController
import androidx.preference.PreferenceCategory
import com.google.android.material.chip.Chip
import me.simpleHook.R
import me.simpleHook.core.base.BasePreferenceFragment
import me.simpleHook.data.ExtBlockDialog
import me.simpleHook.databinding.LayoutInputKeywordBinding
import me.simpleHook.core.extension.addPreferences
import me.simpleHook.core.extension.showPopup
import me.simpleHook.core.ui.custom.ChipPreference
import me.simpleHook.core.ui.custom.MaterialSwitchPreference
import me.simpleHook.core.ui.custom.customDialog
import me.simpleHook.core.ui.custom.exitDialog
import me.simpleHook.core.ui.view.edit.InputView
import me.simpleHook.feature.extension.viewmodel.ExViewModel

class DisableDialogFragment : BasePreferenceFragment() {
    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        requireActivity().findNavController(R.id.nav_host_fragment)
    }

    private lateinit var dialogBlockConfig: ExtBlockDialog

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        dialogBlockConfig = exViewModel.extensionConfig.value?.popupConfig?.blockDialog?.copy()
            ?: throw NullPointerException("DialogCancel is null...")
        val keywordSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_dialog_cancel_title_keyword)
            isIconSpaceReserved = false
            summary = getString(R.string.extension_dialog_cancel_summary_keyword)
            isChecked = dialogBlockConfig.keywordEnable
            setOnPreferenceChangeListener { _, newValue ->
                dialogBlockConfig.keywordEnable = newValue as Boolean
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
            chipTexts = dialogBlockConfig.keywords
        }
        val idSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_dialog_cancel_title_id)
            isIconSpaceReserved = false
            isChecked = dialogBlockConfig.idEnable
            summary = getString(R.string.extension_dialog_cancel_summary_id)
            setOnPreferenceChangeListener { _, newValue ->
                dialogBlockConfig.idEnable = newValue as Boolean
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
            chipTexts = dialogBlockConfig.ids
        }

        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.label_dialog)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.addPreferences(
            keywordSwitch,
            addKeyWords,
            chipPreference,
            idSwitch,
            addIds,
            idChipPreference
        )
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
        customDialog(
            requireContext(),
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


    @SuppressLint("ResourceAsColor")
    private fun showAddIdDialog(chip: Chip? = null) {
        val chipGroup = findPreference<ChipPreference>(KEY_CHIP_GROUP)?.chipGroup
        val inputView = InputView(requireContext()).apply {
            textInputLayout.hint =
                context.getString(R.string.extension_dialog_cancel_hint_dec_or_hex)
        }
        chip?.let { inputView.editText.setText(chip.text) }
        customDialog(
            requireContext(),
            title = getString(R.string.extension_dialog_cancel_add_id),
            contentView = inputView,
            okText = getString(R.string.dialog_confirm),
            okClick = {
                val text = inputView.editText.text.toString().trim()
                if (text.isEmpty()) requireActivity().showPopup(getString(R.string.extension_dialog_cancel_id_is_empty))
                val id = formatId(text)
                id?.let { str ->
                    chip?.let {
                        chip.text = str
                    } ?: findPreference<ChipPreference>(KEY_CHIP_GROUP_ID)?.addChip(str)
                }
                    ?: requireActivity().showPopup(getString(R.string.extension_dialog_cancel_illegal_format))
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
        return dialogBlockConfig == exViewModel.extensionConfig.value!!.popupConfig.blockDialog
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
                "chip is null"
            )
        val idChipGroup = findPreference<ChipPreference>(KEY_CHIP_GROUP_ID)?.chipGroup
            ?: throw NullPointerException("chip is null")

        dialogBlockConfig.keywords = keywordChipGroup.children.map {
            (it as Chip).text.toString()
        }.toList()

        dialogBlockConfig.ids = idChipGroup.children.map {
            (it as Chip).text.toString()
        }.toList()

        exViewModel.updateDialogBlock(dialogBlockConfig)
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