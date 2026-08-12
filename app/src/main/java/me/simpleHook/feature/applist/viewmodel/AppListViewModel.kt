package me.simpleHook.feature.applist.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import me.simpleHook.data.AppListItem
import me.simpleHook.core.constant.Constant.APP_LIST_BY_INSTALLED_TIME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_NAME
import me.simpleHook.core.constant.Constant.APP_LIST_BY_PACKAGE_NAME
import me.simpleHook.core.extension.verCode
import me.simpleHook.core.utils.AppUtil
import me.simpleHook.core.utils.TimeUtil

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private var allUserApps = emptyList<AppListItem>()
    private var allSystemApps = emptyList<AppListItem>()

    private val appNames = ConcurrentHashMap<String, String>()
    private var fetchJob: Job? = null
    private var filterJob: Job? = null
    private var fetchToken = 0
    private var filterToken = 0

    val userApps = MutableLiveData<List<AppListItem>>(emptyList())
    val systemApps = MutableLiveData<List<AppListItem>>(emptyList())
    val isLoading = MutableLiveData(false)

    val queryPattern = MutableLiveData("")

    private val _selectAppListItem = MutableLiveData<AppListItem>()
    val selectAppListItem: LiveData<AppListItem>
        get() = _selectAppListItem

    fun updateSelectApp(appListItem: AppListItem) {
        _selectAppListItem.value = appListItem
    }

    fun fetchData(sortSelected: Int, reverseChecked: Boolean) {
        fetchJob?.cancel()
        filterJob?.cancel()
        val currentFetchToken = ++fetchToken
        fetchJob = viewModelScope.launch {
            isLoading.value = true
            try {
                val packageGroups = loadPackageGroups()
                val initialQuery = queryPattern.value.orEmpty()
                val resolveNamesBeforePublish =
                    sortSelected == APP_LIST_BY_NAME || initialQuery.isNotEmpty()

                val firstGroups = buildAppGroups(
                    packageGroups = packageGroups,
                    resolveNames = resolveNamesBeforePublish,
                    sortSelected = sortSelected,
                    reverseChecked = reverseChecked
                )
                if (currentFetchToken != fetchToken) return@launch

                updateAllApps(firstGroups)
                publishFilteredApps(queryPattern.value.orEmpty(), ++filterToken)
                isLoading.value = false

                if (!resolveNamesBeforePublish && firstGroups.hasMissingNames()) {
                    val completedGroups = buildAppGroups(
                        packageGroups = packageGroups,
                        resolveNames = true,
                        sortSelected = sortSelected,
                        reverseChecked = reverseChecked
                    )
                    if (currentFetchToken != fetchToken) return@launch

                    updateAllApps(completedGroups)
                    publishFilteredApps(queryPattern.value.orEmpty(), ++filterToken)
                }
            } finally {
                if (currentFetchToken == fetchToken) {
                    isLoading.value = false
                }
            }
        }
    }

    fun filerAppItems(pattern: String) {
        val normalizedPattern = pattern.trim()
        queryPattern.value = normalizedPattern
        filterJob?.cancel()
        val currentFilterToken = ++filterToken
        filterJob = viewModelScope.launch {
            publishFilteredApps(normalizedPattern, currentFilterToken)
        }
    }

    suspend fun resolveAppName(appListItem: AppListItem): String {
        if (appListItem.name.isNotEmpty()) return appListItem.name
        appNames[appListItem.packageName]?.let { return it }
        return withContext(Dispatchers.IO) {
            normalizeAppName(AppUtil.getAppName(appListItem.packageName), appListItem.packageName)
                .also { appNames[appListItem.packageName] = it }
        }
    }

    private suspend fun loadPackageGroups(): PackageGroups = withContext(Dispatchers.IO) {
        val packages = AppUtil.getApps()
        currentCoroutineContext().ensureActive()
        val (systemPackages, userPackages) = packages.partition { packageInfo ->
            val flags = packageInfo.applicationInfo?.flags ?: 0
            flags and ApplicationInfo.FLAG_SYSTEM != 0
        }
        PackageGroups(
            userPackages = userPackages,
            systemPackages = systemPackages
        )
    }

    private suspend fun buildAppGroups(
        packageGroups: PackageGroups,
        resolveNames: Boolean,
        sortSelected: Int,
        reverseChecked: Boolean
    ): AppGroups = withContext(Dispatchers.IO) {
        val userAppsDeferred = async {
            packageGroups.userPackages.toAppItems(resolveNames, sortSelected, reverseChecked)
        }
        val systemAppsDeferred = async {
            packageGroups.systemPackages.toAppItems(resolveNames, sortSelected, reverseChecked)
        }
        AppGroups(
            userApps = userAppsDeferred.await(),
            systemApps = systemAppsDeferred.await()
        )
    }

    private suspend fun List<PackageInfo>.toAppItems(
        resolveNames: Boolean,
        sortSelected: Int,
        reverseChecked: Boolean
    ): List<AppListItem> {
        val context = currentCoroutineContext()
        val appItems = map { packageInfo ->
            context.ensureActive()
            getAppItem(packageInfo, resolveNames)
        }
        return getSortAppList(appItems, sortSelected, reverseChecked)
    }

    private fun getAppItem(packageInfo: PackageInfo, resolveName: Boolean): AppListItem {
        val appName = if (resolveName) {
            getAppNameOrPut(packageInfo)
        } else {
            appNames[packageInfo.packageName].orEmpty()
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
        appNames[packageInfo.packageName] ?: normalizeAppName(
            AppUtil.getAppName(packageInfo),
            packageInfo.packageName
        ).also {
            appNames[packageInfo.packageName] = it
        }

    private fun normalizeAppName(appName: String, packageName: String): String {
        return appName.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: packageName
    }

    private fun updateAllApps(appGroups: AppGroups) {
        allUserApps = appGroups.userApps
        allSystemApps = appGroups.systemApps
    }

    private suspend fun publishFilteredApps(pattern: String, token: Int) {
        val userSource = allUserApps
        val systemSource = allSystemApps
        val (filteredUserApps, filteredSystemApps) = if (pattern.isEmpty()) {
            userSource to systemSource
        } else {
            withContext(Dispatchers.Default) {
                val lowerCasePattern = pattern.lowercase(Locale.getDefault())
                userSource.filterBy(lowerCasePattern) to systemSource.filterBy(lowerCasePattern)
            }
        }

        if (token != filterToken) return
        userApps.value = filteredUserApps
        systemApps.value = filteredSystemApps
    }

    private suspend fun List<AppListItem>.filterBy(pattern: String): List<AppListItem> {
        val context = currentCoroutineContext()
        return filter { appItem ->
            context.ensureActive()
            appItem.packageName.lowercase(Locale.getDefault()).contains(pattern) ||
                    appItem.name.lowercase(Locale.getDefault()).contains(pattern)
        }
    }

    private fun getSortAppList(
        appList: List<AppListItem>, sortSelected: Int, reverseChecked: Boolean
    ): List<AppListItem> {
        val tempList = when (sortSelected) {
            APP_LIST_BY_NAME -> appList.sortedBy { it.name.lowercase(Locale.getDefault()) }
            APP_LIST_BY_PACKAGE_NAME -> appList.sortedBy {
                it.packageName.lowercase(Locale.getDefault())
            }
            APP_LIST_BY_INSTALLED_TIME -> appList.sortedBy { it.installedTime }
            else -> appList.sortedBy { it.targetApi }
        }
        return if (reverseChecked) tempList.reversed() else tempList
    }

    private data class PackageGroups(
        val userPackages: List<PackageInfo>,
        val systemPackages: List<PackageInfo>
    )

    private data class AppGroups(
        val userApps: List<AppListItem>,
        val systemApps: List<AppListItem>
    ) {
        fun hasMissingNames(): Boolean {
            return userApps.any { it.name.isEmpty() } || systemApps.any { it.name.isEmpty() }
        }
    }
}
