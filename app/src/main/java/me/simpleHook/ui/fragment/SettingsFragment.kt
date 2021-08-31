package me.simpleHook.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import me.simpleHook.BuildConfig
import me.simpleHook.R
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.util.toast

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
            summary = BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"
        }
    }

}