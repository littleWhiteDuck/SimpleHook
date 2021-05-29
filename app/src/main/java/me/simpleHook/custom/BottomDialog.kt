package me.simpleHook.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
import me.simpleHook.R
import me.simpleHook.database.AppConfigEntity
import me.simpleHook.databinding.CustomBottomPopupBinding
import me.simpleHook.utils.AppUtils
import me.simpleHook.utils.toast

@SuppressLint("ViewConstructor")
class BottomDialog(
    context: Context,
    private val appConfigEntity: AppConfigEntity,
    private val onClick: () -> Unit
) : BottomPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.custom_bottom_popup
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate() {
        super.onCreate()
        val binding = CustomBottomPopupBinding.bind(popupImplView)
        binding.apply {
            appConfigEntity.apply {
                appNameText.text = appName
                packageNameText.text = packageName
                configDescription.text = description
                applicableVersion.text = "支持：$versionName"
                appIcon.setImageDrawable(AppUtils.getIcon(context, packageName))
                currentVersion.text = "当前：${AppUtils.getAppVersionName(context, packageName)}"
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
                        "出现错误".toast(context)
                    }
                }
            }
        }
    }

    override fun getMaxHeight(): Int {
        return XPopupUtils.getWindowWidth(context) * 0.85.toInt()
    }
}