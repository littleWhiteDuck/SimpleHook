package me.simpleHook.ui.fragment.extension

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.preference.PreferenceCategory
import me.simpleHook.R
import me.simpleHook.base.BasePreferenceFragment
import me.simpleHook.data.ExtRecordSettings
import me.simpleHook.extension.addPreferences
import me.simpleHook.ui.custom.MaterialSwitchPreference
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.viewmodel.ExViewModel

class RecordSettingsFragment : BasePreferenceFragment() {
    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        requireActivity().findNavController(R.id.nav_host_fragment)
    }
    private lateinit var recordSettings: ExtRecordSettings

    override fun init() {
        setDividerHeight(0)
        initMenu()
    }

    override fun canBack(): Boolean {
        return recordSettings == exViewModel.extensionConfig.value!!.recordSettings
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        recordSettings = exViewModel.extensionConfig.value?.recordSettings?.copy()
            ?: throw NullPointerException("Record Config is null...")
        val stack = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_record_title_add_stack)
            isIconSpaceReserved = false
            isChecked = recordSettings.enableStack
            setOnPreferenceChangeListener { _, newValue ->
                recordSettings.enableStack = newValue as Boolean
                true
            }
        }
        val base64 = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_record_title_add_base64)
            isIconSpaceReserved = false
            isChecked = recordSettings.enableBase64
            setOnPreferenceChangeListener { _, newValue ->
                recordSettings.enableBase64 = newValue as Boolean
                true
            }
        }
        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.extension_record_record)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.addPreferences(stack, base64)
        setPreferenceScreen(preferenceScreen)
    }

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_file_monitor, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_save -> {
                        saveConfig(true)
                    }
                }
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveConfig(exit: Boolean) {
        exViewModel.updateRecordSettings(recordSettings = recordSettings)
        if (exit) navController.navigateUp()
    }
}