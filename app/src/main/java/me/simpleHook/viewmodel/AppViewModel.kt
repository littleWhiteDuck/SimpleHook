@file:Suppress("RedundantSuspendModifier")

package me.simpleHook.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.simpleHook.bean.AppItem
import me.simpleHook.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.extension.verCode
import me.simpleHook.util.AppUtils
import me.simpleHook.util.TimeUtil

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _userApps = MutableLiveData<List<AppItem>>()
    private val _systemApps = MutableLiveData<List<AppItem>>()

    private val appNames = HashMap<String, String>()

    val userApps = MutableLiveData<List<AppItem>>(emptyList())
    val systemApps = MutableLiveData<List<AppItem>>(emptyList())

    val queryPattern = MutableLiveData("")

    private val _selectAppItem = MutableLiveData<AppItem>()
    val selectAppItem: LiveData<AppItem>
        get() = _selectAppItem

    fun updateSelectApp(appItem: AppItem) {
        _selectAppItem.value = appItem
    }

    fun fetchData(sortSelected: Int, reverseChecked: Boolean) = viewModelScope.launch {
        val userAppsDeferred = async(Dispatchers.IO) { fetchUserData(sortSelected, reverseChecked) }
        val systemAppsDeferred =
            async(Dispatchers.IO) { fetchSystemData(sortSelected, reverseChecked) }

        val userApps = userAppsDeferred.await()
        val systemApps = systemAppsDeferred.await()

        _userApps.value = userApps
        _systemApps.value = systemApps
        filerAppItems(queryPattern.value!!)
        if (sortSelected != APP_LIST_BY_NAME) {
            val userAppsDeferred2 = async(Dispatchers.IO) {
                AppUtils.getInstalledUserApp(getApplication()).map { packageInfo ->
                    getAppNameOrPut(packageInfo)
                    getAppItem(packageInfo = packageInfo, sortSelected = sortSelected)
                }
            }

            val systemAppsDeferred2 = async(Dispatchers.IO) {
                AppUtils.getInstalledSystemApp(getApplication()).map { packageInfo ->
                    getAppNameOrPut(packageInfo)
                    getAppItem(packageInfo = packageInfo, sortSelected = sortSelected)
                }
            }

            val userApps2 = userAppsDeferred2.await()
            val systemApps2 = systemAppsDeferred2.await()
            _userApps.value = userApps2
            _systemApps.value = systemApps2
        }
    }

    private suspend fun fetchUserData(sortSelected: Int, reverseChecked: Boolean): List<AppItem> {
        val packageInfoList = AppUtils.getInstalledUserApp(getApplication())
        return packageInfoList.map { packageInfo ->
            getAppItem(packageInfo, sortSelected)
        }.let { useApps ->
            getSortAppList(useApps, sortSelected, reverseChecked)
        }
    }

    private suspend fun fetchSystemData(sortSelected: Int, reverseChecked: Boolean): List<AppItem> {
        val packageInfoList = AppUtils.getInstalledSystemApp(getApplication())
        return packageInfoList.map { packageInfo ->
            getAppItem(packageInfo, sortSelected)
        }.let { useApps ->
            getSortAppList(useApps, sortSelected, reverseChecked)
        }
    }

    private fun getAppItem(packageInfo: PackageInfo, sortSelected: Int): AppItem {
        val isSortByAppName = sortSelected == APP_LIST_BY_NAME
        val appName = if (isSortByAppName) {
            getAppNameOrPut(packageInfo)
        } else {
            appNames[packageInfo.packageName] ?: ""
        }
        return AppItem(
            name = appName,
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "null",
            versionCode = packageInfo.verCode.toString(),
            installedTime = TimeUtil.getTime(packageInfo.lastUpdateTime, "yyyy-MM-dd HH:mm:ss"),
            targetApi = packageInfo.applicationInfo?.targetSdkVersion ?: -1
        )
    }

    private fun getAppNameOrPut(packageInfo: PackageInfo) =
        appNames[packageInfo.packageName] ?: AppUtils.getAppName(
            getApplication(),
            packageInfo.packageName
        ).also {
            appNames[packageInfo.packageName] = it
        }

    fun filerAppItems(pattern: String) = viewModelScope.launch {
        if (_userApps.value == null) return@launch
        if (pattern.isEmpty()) {
            userApps.value = _userApps.value
            systemApps.value = _systemApps.value
        } else {
            val filter1 = withContext(Dispatchers.IO) {
                _userApps.value?.filter {
                    it.packageName.contains(pattern) || it.name.contains(pattern, true)
                }
            }
            val filter2 = withContext(Dispatchers.IO) {
                _systemApps.value?.filter {
                    it.packageName.contains(pattern) || it.name.contains(pattern, true)
                }
            }
            userApps.value = filter1 ?: emptyList()
            systemApps.value = filter2 ?: emptyList()
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