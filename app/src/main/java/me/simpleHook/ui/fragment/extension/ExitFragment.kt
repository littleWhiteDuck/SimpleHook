package me.simpleHook.ui.fragment.extension

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.preference.PreferenceCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.base.BasePreferenceFragment
import me.simpleHook.data.Exit
import me.simpleHook.ui.custom.MaterialSwitchPreference
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.viewmodel.ExViewModel

class ExitFragment : BasePreferenceFragment() {

    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
    }

    private lateinit var tempConfig: Exit

    override fun init() {
        setDividerHeight(0)
        initMenu()
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
        exViewModel.extensionConfig.value!!.exit.info = Json.encodeToString(tempConfig)
        if (exit) navController.navigateUp()
    }

    override fun canBack(): Boolean {
        return Json.encodeToString(tempConfig) == exViewModel.extensionConfig.value!!.exit.info
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        tempConfig = Json.decodeFromString(exViewModel.extensionConfig.value!!.exit.info)
        val finishSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_finish)
            isIconSpaceReserved = false
            isChecked = tempConfig.finish
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.finish = newValue as Boolean
                true
            }
        }
        val exitSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_exit)
            isIconSpaceReserved = false
            isChecked = tempConfig.exit
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.exit = newValue as Boolean
                true
            }
        }

        val killSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_kill_process)
            isIconSpaceReserved = false
            isChecked = tempConfig.kill
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.kill = newValue as Boolean
                true
            }
        }

        val recordCrashSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_exit_title_record_crash)
            summary = getString(R.string.extension_exit_summary_record_crash)
            isIconSpaceReserved = false
            isChecked = tempConfig.recordCrash
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.recordCrash = newValue as Boolean
                true
            }
        }
        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.extension_exit_title_app_exit)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.apply {
            addPreference(finishSwitch)
            addPreference(exitSwitch)
            addPreference(killSwitch)
            addPreference(recordCrashSwitch)
        }
        setPreferenceScreen(preferenceScreen)
    }
}