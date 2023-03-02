package me.simpleHook.ui.fragment.backup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.GlobalValue
import me.simpleHook.R
import me.simpleHook.base.BaseBottomFragment
import me.simpleHook.compat.DocumentCompat
import me.simpleHook.ui.view.backup.BackupRestoreView
import me.simpleHook.ui.view.backup.RestoreItemView
import me.simpleHook.util.TimeUtil
import me.simpleHook.worker.CloudBackupHelper


class BackupRestoreFragment(
    val callBackLocal: (RestoreItem) -> Unit, val callBackCloud: (RestoreCloudItem) -> Unit
) : BaseBottomFragment<BackupRestoreView>() {
    private val adapter = MultiTypeAdapter()
    override fun initRootView(): BackupRestoreView {
        return BackupRestoreView(requireContext())
    }

    @SuppressLint("CheckResult")
    override fun init() {
        adapter.register(RestoreLocalItemViewDelegate {
            callBackLocal(it)
            dismiss()
        })
        adapter.register(RestoreCloudItemViewDelegate {
            callBackCloud(it)
            dismiss()
        })
        root.listView.adapter = adapter
        initData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val list: List<Any> = if (tag == "RESTORE_LOCAL") {
                getLocalBackups()
            } else {
                getCloudBackups()
            }
            withContext(Dispatchers.Main) {
                adapter.items = list
                adapter.notifyDataSetChanged()
                root.progressBar.hide()
            }
        }
    }

    private fun getCloudBackups(): List<RestoreCloudItem> {
        val list = ArrayList<RestoreCloudItem>()
        val backups = CloudBackupHelper.getBackups()
        backups.sortedByDescending { it.modified }.forEach {
            if (it.contentLength != 0L && it.name.contains("shbackup")) {
                list.add(RestoreCloudItem(it.name,
                    CloudBackupHelper.getUriByName(it.name),
                    TimeUtil.calculateRangeToNow(requireContext(), it.modified.time)))
            }

        }
        return list
    }

    private fun getLocalBackups(): List<RestoreItem> {
        val backupFile = DocumentCompat.getDocumentFile(requireContext(),
            Uri.parse(GlobalValue.sp.backup_path),
            "SimpleHook/Backups") ?: return emptyList()
        val listFiles = backupFile.listFiles()
        val list = ArrayList<RestoreItem>()
        listFiles.sortedByDescending {
            it.lastModified()
        }.forEach { file ->
            file.name?.let {
                if (it.contains("shbackup")) {
                    list.add(RestoreItem(it,
                        file.uri,
                        TimeUtil.calculateRangeToNow(requireContext(), file.lastModified())))
                }
            }
        }
        return list
    }

}

data class RestoreItem(val name: String, val uri: Uri, val time: String)

data class RestoreCloudItem(val name: String, val url: String, val time: String)

class RestoreLocalItemViewDelegate(val onClick: (RestoreItem) -> Unit) :
    ViewDelegate<RestoreItem, RestoreItemView>() {
    override fun onBindView(view: RestoreItemView, item: RestoreItem) {
        view.desc.text = item.name
        view.time.text = item.time
        view.icon.setImageResource(R.drawable.ic_storage_24)
        view.setOnClickListener {
            onClick(item)
        }
    }

    override fun onCreateView(context: Context): RestoreItemView {
        return RestoreItemView(context)
    }
}

class RestoreCloudItemViewDelegate(val onClickCloud: (RestoreCloudItem) -> Unit) :
    ViewDelegate<RestoreCloudItem, RestoreItemView>() {
    override fun onBindView(view: RestoreItemView, item: RestoreCloudItem) {
        view.desc.text = item.name
        view.time.text = item.time
        view.icon.setImageResource(R.drawable.ic_cloud_done_24)
        view.setOnClickListener {
            onClickCloud(item)
        }
    }

    override fun onCreateView(context: Context): RestoreItemView {
        return RestoreItemView(context)
    }
}