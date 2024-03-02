package me.simpleHook.ui.fragment.extension

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation.findNavController
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.simpleHook.R
import me.simpleHook.base.BasePreferenceFragment
import me.simpleHook.bean.FileMonitorConfig
import me.simpleHook.extension.addPreferences
import me.simpleHook.ui.custom.MaterialSwitchPreference
import me.simpleHook.ui.custom.exitDialog
import me.simpleHook.viewmodel.ExViewModel


class FileMonitorFragment : BasePreferenceFragment() {
    private val exViewModel by activityViewModels<ExViewModel>()
    private val navController by lazy {
        findNavController(requireActivity(), R.id.nav_host_fragment)
    }

    //    private val tempConfig by lazy { exViewModel.extensionConfig.value?.fileMonitor ?: throw NullPointerException("FileMonitorConfig is null...") }
    private lateinit var tempConfig: FileMonitorConfig
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val fileMonitorInfo = exViewModel.extensionConfig.value?.fileMonitor?.info
            ?: throw NullPointerException("FileMonitorConfig is null...")
        tempConfig = Json.decodeFromString(fileMonitorInfo)
        val createFile = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_create_file)
            isIconSpaceReserved = false
            isChecked = tempConfig.createFile
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.createFile = newValue as Boolean
                true
            }
        }
        val deleteFile = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_delete_file)
            isIconSpaceReserved = false
            isChecked = tempConfig.deleteFile
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.deleteFile = newValue as Boolean
                true
            }
        }
        val outputFile = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_write_file)
            isIconSpaceReserved = false
            isChecked = tempConfig.outputFile
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.outputFile = newValue as Boolean
                true
            }
        }
        val inputFile = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_read_file)
            isIconSpaceReserved = false
            isChecked = tempConfig.inputFile
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.inputFile = newValue as Boolean
                true
            }
        }
        val assetsFile = MaterialSwitchPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_read_assets_file)
            isIconSpaceReserved = false
            isChecked = tempConfig.assetsFile
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.assetsFile = newValue as Boolean
                true
            }
        }
        val seekBarPreference = SeekBarPreference(requireContext()).apply {
            isPersistent = false
            title = getString(R.string.extension_file_set_cache_size)
            summary = getString(R.string.extension_file_summary_set_cache_size)
            isIconSpaceReserved = false
            max = 512
            value = tempConfig.cacheSize
            showSeekBarValue = true
            setOnPreferenceChangeListener { _, newValue ->
                tempConfig.cacheSize = newValue as Int
                true
            }
        }

        val preferenceCategory = PreferenceCategory(requireContext()).apply {
            title = getString(R.string.extension_file_operation)
            isIconSpaceReserved = false
        }
        val preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen.addPreference(preferenceCategory)
        preferenceCategory.addPreferences(
            createFile,
            deleteFile,
            inputFile, outputFile, assetsFile, seekBarPreference
        )
        setPreferenceScreen(preferenceScreen)
    }

    override fun init() {
        setDividerHeight(0)
        initMenu()
    }

    override fun canBack(): Boolean {
        return Json.encodeToString(tempConfig) == exViewModel.extensionConfig.value!!.fileMonitor.info
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
        exViewModel.extensionConfig.value!!.fileMonitor.info = Json.encodeToString(tempConfig)
        if (exit) navController.navigateUp()
    }
}