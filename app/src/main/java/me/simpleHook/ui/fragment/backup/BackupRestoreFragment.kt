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

class BackupRestoreFragment(val callBack: (RestoreItem) -> Unit) :
    BaseBottomFragment<BackupRestoreView>() {
    private val adapter = MultiTypeAdapter()
    override fun initRootView(): BackupRestoreView {
        return BackupRestoreView(requireContext())
    }

    @SuppressLint("CheckResult")
    override fun init() {
        adapter.register(RestoreItemViewDelegate {
            callBack(it)
            dismiss()
        })
        root.listView.adapter = adapter
        initData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val backupFile = DocumentCompat.getDocumentFile(requireContext(),
                Uri.parse(GlobalValue.sp.backup_path),
                "SimpleHook/Backups") ?: return@launch
            val listFiles = backupFile.listFiles()
            val list = ArrayList<RestoreItem>()
            listFiles.forEach { file ->
                file.name?.let {
                    list.add(RestoreItem(it,
                        file.uri,
                        TimeUtil.calculateRangeToNow(requireContext(), file.lastModified())))
                }
            }
            withContext(Dispatchers.Main) {
                adapter.items = list.sortedBy {
                    it.time
                }.reversed()
                adapter.notifyDataSetChanged()
            }
        }
    }

}

data class RestoreItem(val name: String, val uri: Uri, val time: String)


class RestoreItemViewDelegate(val onClick: (RestoreItem) -> Unit) :
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