package me.simpleHook.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.simpleHook.R
import me.simpleHook.bean.AppConfigBean
import me.simpleHook.bean.ConfigItem
import me.simpleHook.database.AppViewModel
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.ui.activity.AboutActivity
import me.simpleHook.util.AppUtils
import me.simpleHook.util.JsonUtil
import me.simpleHook.util.ToolUtils
import me.simpleHook.util.toast
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: AppViewModel by activityViewModels()

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
            summary = "${
                AppUtils.getAppVersionName(
                    requireContext(),
                    "me.simpleHook"
                )
            }(${AppUtils.getAppVersionCode(requireContext(), "me.simpleHook")})"
        }
        findPreference<Preference>("help")?.apply {
            setOnPreferenceClickListener {
                showHelp()
                true
            }
        }
        findPreference<Preference>("backConfigs")?.apply {
            setOnPreferenceClickListener {
                backConfigs()
                true
            }
        }
        findPreference<Preference>("restoreConfigs")?.apply {
            setOnPreferenceClickListener {
                restoreConfigs()
                true
            }
        }
        findPreference<Preference>("importOldConfig")?.apply {
            setOnPreferenceClickListener {
                importOldConfig()
                true
            }
        }
    }

    private fun importOldConfig() {
        val strClip = ToolUtils.getClipboardContent(requireContext()) ?: ""
        if (strClip.contains("configs")||strClip.contains("\"enable\":"))"请勿导入新版配置".toast(requireContext())
        try {
            if (JsonUtil.isJsonObject(strClip)) {
                val appConfigBean = Gson().fromJson(strClip, AppConfigBean::class.java)
                appConfigBean.apply {
                    viewModel.insertConfigs(
                        AppConfig(
                            packageName,
                            appName,
                            versionName,
                            description,
                            Gson().toJson(config).toString(),
                            canUse
                        )
                    )
                }

            } else if (JsonUtil.isJsonArray(strClip)) {
                val type = object : TypeToken<List<AppConfigBean>>(){}.type
                val configBeans = Gson().fromJson<List<AppConfigBean>>(strClip, type)
                val configItems = ArrayList<ConfigItem>()
                configBeans.forEach{
                    it.apply {
                        configItems.add(
                            ConfigItem(AppConfig(packageName, appName, versionName, description, Gson().toJson(config).toString(), canUse))
                        )
                    }
                }
                ConfigDialogFragment(configItems).show(requireActivity().supportFragmentManager, "importOld")
            }
        }catch (e: Exception){
            getString(R.string.error_tip).toast(requireContext())
        }

    }

    private fun showHelp() {
        val bufferedReader =
            BufferedReader(InputStreamReader(requireActivity().assets.open("help")))
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

    private fun restoreConfigs() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        startActivityForResult(intent, 2)

    }

    private fun backConfigs() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "back.json")
        startActivityForResult(intent, 3)

    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                // Get the position of the view in the recycler view
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) {
                    return
                }

                if (position == parent.adapter!!.itemCount - 1) {
                    // Add padding to the last item. You should probably use a @dimen resource.
                    outRect.bottom = 200
                }
            }
        })
        return recyclerView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            val readText = uri?.let { readTextFromUri(it) } ?: ""
            if (readText == "") {
                Toast.makeText(requireContext(), "错误", Toast.LENGTH_SHORT).show()
            } else {
                importConfigs(readText)
            }
        } else if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.apply {
                thread {
                    alterDocument(this, JsonUtil.formatJson(Gson().toJson(viewModel.getConfigs())))
                }

            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            requireActivity().contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream))).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            "error".toast(requireContext())
        }

        return stringBuilder.toString()
    }

    private fun importConfigs(configs: String) {
        try {
            when {
                JsonUtil.isJsonArray(configs) -> {
                    val dataList = JsonUtil.importConfigs(configs)
                    if (dataList.isEmpty()) {
                        getString(R.string.main_home_import_incorrect_format_tip).toast(
                            requireContext()
                        )
                        return
                    } else {
                        ConfigDialogFragment(dataList as ArrayList<ConfigItem>).show(
                            requireActivity().supportFragmentManager,
                            "import"
                        )
                    }
                }
                JsonUtil.isJsonObject(configs) -> {
                    val appConfig = Gson().fromJson(configs, AppConfig::class.java)
                    viewModel.insertConfigs(appConfig)
                }
            }
        } catch (e: java.lang.Exception) {
            getString(R.string.main_home_import_incorrect_format_tip).toast(requireContext())
        }
    }

    private fun alterDocument(uri: Uri, strConfigs: String) {
        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                // use{} lets the document provider know you're done by automatically closing the stream
                FileOutputStream(it.fileDescriptor).use { put ->
                    put.write((strConfigs).toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}