@file:Suppress("RedundantSuspendModifier")

package me.simpleHook.feature.applist.viewmodel

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
import me.simpleHook.data.AppListItem
import me.simpleHook.core.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.core.extension.verCode
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.TimeUtil

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _userApps = MutableLiveData<List<AppListItem>>()
    private val _systemApps = MutableLiveData<List<AppListItem>>()

    private val appNames = HashMap<String, String>()

    val userApps = MutableLiveData<List<AppListItem>>(emptyList())
    val systemApps = MutableLiveData<List<AppListItem>>(emptyList())

    val queryPattern = MutableLiveData("")

    private val _selectAppListItem = MutableLiveData<AppListItem>()
    val selectAppListItem: LiveData<AppListItem>
        get() = _selectAppListItem

    fun updateSelectApp(appListItem: AppListItem) {
        _selectAppListItem.value = appListItem
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
                AppUtil.getInstalledUserApp().map { packageInfo ->
                    getAppNameOrPut(packageInfo)
                    getAppItem(packageInfo = packageInfo, sortSelected = sortSelected)
                }
            }

            val systemAppsDeferred2 = async(Dispatchers.IO) {
                AppUtil.getInstalledSystemApp().map { packageInfo ->
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

    private suspend fun fetchUserData(sortSelected: Int, reverseChecked: Boolean): List<AppListItem> {
        val packageInfoList = AppUtil.getInstalledUserApp()
        return packageInfoList.map { packageInfo ->
            getAppItem(packageInfo, sortSelected)
        }.let { useApps ->
            getSortAppList(useApps, sortSelected, reverseChecked)
        }
    }

    private suspend fun fetchSystemData(sortSelected: Int, reverseChecked: Boolean): List<AppListItem> {
        val packageInfoList = AppUtil.getInstalledSystemApp()
        return packageInfoList.map { packageInfo ->
            getAppItem(packageInfo, sortSelected)
        }.let { useApps ->
            getSortAppList(useApps, sortSelected, reverseChecked)
        }
    }

    private fun getAppItem(packageInfo: PackageInfo, sortSelected: Int): AppListItem {
        val isSortByAppName = sortSelected == APP_LIST_BY_NAME
        val appName = if (isSortByAppName) {
            getAppNameOrPut(packageInfo)
        } else {
            appNames[packageInfo.packageName] ?: ""
        }
        return AppListItem(
            name = appName,
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "null",
            versionCode = packageInfo.verCode.toString(),
            installedTime = TimeUtil.getTime(packageInfo.lastUpdateTime, "yyyy-MM-dd HH:mm:ss"),
            targetApi = packageInfo.applicationInfo?.targetSdkVersion ?: -1
        )
    }

    private fun getAppNameOrPut(packageInfo: PackageInfo) =
        appNames[packageInfo.packageName] ?: AppUtil.getAppName(
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
        appList: List<AppListItem>, sortSelected: Int, reverseChecked: Boolean
    ): List<AppListItem> {
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