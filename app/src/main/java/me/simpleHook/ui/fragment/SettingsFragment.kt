package me.simpleHook.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.util.AppUtils
import me.simpleHook.util.toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<SwitchPreferenceCompat>("openStorage")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                me.simpleHook.util.FileUtils.verifyStoragePermissions(requireActivity())
            }
            true
        }
        findPreference<SwitchPreferenceCompat>("openXml")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                "支持支持' New XSharedPreferences '的框架，如 LSPosed、EdXposed".toast(requireContext())
            }
            true
        }
        findPreference<Preference>("about")?.apply {
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                true
            }
            summary = "${AppUtils.getAppVersionName(requireContext(), "me.simpleHook")}(${AppUtils.getAppVersionCode(requireContext(), "me.simpleHook")})"
        }
        findPreference<Preference>("help")?.apply {
            setOnPreferenceClickListener {
                showHelp()
                true
            }
        }
    }

    private fun showHelp(){
        val bufferedReader = BufferedReader(InputStreamReader(requireActivity().assets.open("help")))
        val message = try {
            var msg = ""
            bufferedReader.readLines().forEach {
                msg += it + "\n"
            }
            msg.substring(0, msg.length - 1)
        } catch (e: IOException) {
            "失败！"
        } finally {
            bufferedReader.close()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("帮助")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .show()
    }

}