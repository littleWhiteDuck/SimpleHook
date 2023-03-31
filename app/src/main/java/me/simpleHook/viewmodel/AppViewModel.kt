package me.simpleHook.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.bean.AppItem
import me.simpleHook.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.util.AppUtils
import me.simpleHook.util.TimeUtil

class AppViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _userApps = MutableLiveData<List<AppItem>>()
    private val _systemApps = MutableLiveData<List<AppItem>>()
    private val blackList = "me.simpleHook,bin.mt.plus.canary,com.drakeet.purewriter"
    private val hashMap: HashMap<String, String> = HashMap()
    val userApps = MutableLiveData<List<AppItem>>(emptyList())
    val systemApps = MutableLiveData<List<AppItem>>(emptyList())

    val queryPattern = MutableLiveData("")

    private val _selectAppItem = MutableLiveData<AppItem>()
    val selectAppItem: LiveData<AppItem>
        get() = _selectAppItem

    fun updateSelectApp(appItem: AppItem) {
        _selectAppItem.value = appItem
    }

    @Suppress("DEPRECATION")
    fun fetchData(sortSelected: Int, reverseChecked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val packageInfoList = AppUtils.getApps(getApplication())
            val userApps = ArrayList<AppItem>()
            val systemApps = ArrayList<AppItem>()
            packageInfoList.forEach { packageInfo ->
                val packageName = packageInfo.packageName
                if (!blackList.contains(packageName)) {
                    val appName = hashMap[packageName]
                        ?: if (sortSelected == APP_LIST_BY_NAME) AppUtils.getAppName(application,
                            packageInfo) else ""
                    if (appName.isNotEmpty()) {
                        hashMap[packageName] = appName
                    }
                    val appItem = AppItem(appName,
                        packageInfo.packageName,
                        packageInfo.versionName,
                        packageInfo.versionCode.toString(),
                        TimeUtil.getTime(packageInfo.lastUpdateTime, "yyyy-MM-dd HH:mm:ss"),
                        packageInfo.applicationInfo.targetSdkVersion)
                    if ((packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        systemApps.add(appItem)
                    } else {
                        userApps.add(appItem)
                    }
                }
            }
            val tempUserApps = getSortAppList(userApps, sortSelected, reverseChecked)
            val tempSystemApps = getSortAppList(systemApps, sortSelected, reverseChecked)
            withContext(Dispatchers.Main) {
                _userApps.value = tempUserApps
                _systemApps.value = tempSystemApps
                filerAppItems(queryPattern.value!!)
            }
            userApps.clear()
            systemApps.clear()
            tempUserApps.forEach { appItem ->
                val appName = hashMap[appItem.packageName] ?: AppUtils.getAppName(application,
                    appItem.packageName).also {
                    hashMap[appItem.packageName] = it
                }
                userApps.add(appItem.copy(name = appName))
            }
            tempSystemApps.forEach { appItem ->
                val appName = hashMap[appItem.packageName] ?: AppUtils.getAppName(application,
                    appItem.packageName).also {
                    hashMap[appItem.packageName] = it
                }
                systemApps.add(appItem.copy(name = appName))
            }
            withContext(Dispatchers.Main) {
                _userApps.value = userApps
                _systemApps.value = systemApps
            }
        }
    }

    fun filerAppItems(pattern: String) = viewModelScope.launch(Dispatchers.IO) {
        if (_userApps.value == null) return@launch
        if (pattern.isEmpty()) {
            withContext(Dispatchers.Main) {
                userApps.value = _userApps.value
                systemApps.value = _systemApps.value
            }
        } else {
            val filter1 = _userApps.value?.filter {
                it.packageName.contains(pattern) || it.name.contains(pattern, true)
            }
            val filter2 = _systemApps.value?.filter {
                it.packageName.contains(pattern) || it.name.contains(pattern, true)
            }
            withContext(Dispatchers.Main) {
                userApps.value = filter1 ?: emptyList()
                systemApps.value = filter2 ?: emptyList()
            }
        }
    }

    private fun getSortAppList(
        appList: List<AppItem>, sortSelected: Int, reverseChecked: Boolean
    ): List<AppItem> {
        val tempList = appList.sortedBy { appItem ->
            when (sortSelected) {
                APP_LIST_BY_NAME -> appItem.name
                APP_LIST_BY_PACKAGE_NAME -> appItem.packageName
                APP_LIST_BY_INSTALLED_TIME -> appItem.installedTime
                else -> appItem.targetApi.toString()
            }
        }
        return if (reverseChecked) tempList.reversed() else tempList
    }
}