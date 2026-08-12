package me.simpleHook.feature.extension.ui

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
import me.simpleHook.core.base.BasePreferenceFragment
import me.simpleHook.data.ExtExitConfig
import me.simpleHook.core.extension.addPreferences
import me.simpleHook.core.ui.custom.MaterialSwitchPreference
import me.simpleHook.core.ui.custom.exitDialog
import me.simpleHook.feature.extension.viewmodel.ExViewModel

class ExitFragment : BasePreferenceFragment() {

    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        requireActivity().findNavController(R.id.nav_host_fragment)
    }

    private lateinit var exitConfig: ExtExitConfig

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
        exViewModel.updateExit(exitConfig = exitConfig)
        if (exit) navController.navigateUp()
    }

    override fun canBack(): Boolean {
        return exitConfig == exViewModel.extensionConfig.value!!.exitConfig
    }

    override fun notBackTip() {
        exitDialog(requireContext(), okClick = { saveConfig(exit = true) }, neutralClick = {
            backPressed()
        }, cancelClick = {
            saveConfig(false)
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        exitConfig = exViewModel.extensionConfig.value!!.exitConfig.copy()
        val finishSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_finish)
            isIconSpaceReserved = false
            isChecked = exitConfig.finish
            setOnPreferenceChangeListener { _, newValue ->
                exitConfig.finish = newValue as Boolean
                true
            }
        }
        val exitSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_exit)
            isIconSpaceReserved = false
            isChecked = exitConfig.exit
            setOnPreferenceChangeListener { _, newValue ->
                exitConfig.exit = newValue as Boolean
                true
            }
        }

        val killSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_intercept_app_exit_title)
            summary = getString(R.string.extension_intercept_app_exit_kill_process)
            isIconSpaceReserved = false
            isChecked = exitConfig.kill
            setOnPreferenceChangeListener { _, newValue ->
                exitConfig.kill = newValue as Boolean
                true
            }
        }

        val recordCrashSwitch = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_exit_title_record_crash)
            summary = getString(R.string.extension_exit_summary_record_crash)
            isIconSpaceReserved = false
            isChecked = exitConfig.recordCrash
            setOnPreferenceChangeListener { _, newValue ->
                exitConfig.recordCrash = newValue as Boolean
                true
            }
        }
        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.extension_exit_title_app_exit)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.addPreferences(finishSwitch,exitSwitch,killSwitch,recordCrashSwitch)
        setPreferenceScreen(preferenceScreen)
    }
}