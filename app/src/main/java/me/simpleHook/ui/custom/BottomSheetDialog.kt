package me.simpleHook.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.simpleHook.R
import me.simpleHook.database.entity.AppConfig
import me.simpleHook.databinding.CustomBottomPopupBinding
import me.simpleHook.util.AppUtils
import me.simpleHook.util.toast


class BottomSheetDialog(
    context: Context,
    private val appConfig: AppConfig,
    private val onClick: () -> Unit
) : BottomSheetDialog(context) {

    @SuppressLint("SetTextI18n")
    fun setContentView() {

        val binding = CustomBottomPopupBinding.inflate(layoutInflater, null, false)
        binding.apply {
            appConfig.apply {
                appNameText.text = appName
                packageNameText.text = packageName
                configDescription.text = description
                supportVersion.text =
                    context.getString(R.string.main_bottom_support_version, versionName)
                appIcon.setImageDrawable(AppUtils.getIcon(context, packageName))
                currentVersion.text = context.getString(
                    R.string.main_bottom_current_installed_version,
                    AppUtils.getAppVersionName(context, packageName)
                )
                if (configs.startsWith("config://")) editConfigButton.isEnabled = false
                editConfigButton.setOnClickListener {
                    onClick()
                    dismiss()
                }
                downloadButton.setOnClickListener {
                    val uri = Uri.parse("market://details?id=$packageName")
                    val intent = Intent(Intent.ACTION_VIEW, uri).also {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.getString(R.string.error_tip).toast(context)
                    }
                }
                appSummary.setOnClickListener {
                    AppUtils.startApp(packageName, binding.root.context)
                }
            }
        }
        super.setContentView(binding.root)
    }
}