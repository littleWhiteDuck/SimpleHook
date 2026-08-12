package com.github.kyuubiran.ezxhelper.utils.parasitics

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.*
import android.view.KeyEvent
import android.view.MotionEvent
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.*
import com.github.kyuubiran.ezxhelper.utils.Log.logexIfThrow
import java.lang.reflect.*

@SuppressLint("PrivateApi")
object ActivityHelper {

    private var isStubHooked = false

    internal fun initSubActivity() {
        if (isStubHooked) return
        runCatching {
            //region Instrumentation
            val cActivityThread = Class.forName("android.app.ActivityThread")
            val fCurrentActivityThread =
                cActivityThread.staticField("sCurrentActivityThread")
            val sCurrentActivityThread = fCurrentActivityThread.get(null)!!
            val fmInstrumentation =
                cActivityThread.field("mInstrumentation")
            val mGetInstrumentation = cActivityThread.method("getInstrumentation")
            val mInstrumentation =
                mGetInstrumentation.invoke(sCurrentActivityThread)!! as Instrumentation
            fmInstrumentation.set(
                sCurrentActivityThread,
                MyInstrumentation(mInstrumentation)
            )
            Log.d("ActivityProxy Instrumentation part finished")
            //endregion
            //region Handler
            val fmH = cActivityThread.field("mH")
            val originHandler = fmH.get(sCurrentActivityThread) as Handler
            val fHandlerCallback = Handler::class.java.field("mCallback")
            val currHCallback = fHandlerCallback.get(originHandler) as Handler.Callback?
            if (currHCallback == null || currHCallback::class.java.name != MyHandler::class.java.name) {
                fHandlerCallback.set(originHandler, MyHandler(currHCallback))
            }
            Log.d("ActivityProxy Handler part finished")
            //endregion
            //region IActivityManager
            var cActivityManager: Class<*>
            var fgDefault: Field
            try {
                cActivityManager = Class.forName("android.app.ActivityManagerNative")
                fgDefault = cActivityManager.staticField("gDefault")
            } catch (e1: Exception) {
                try {
                    cActivityManager = Class.forName("android.app.ActivityManager")
                    fgDefault = cActivityManager.staticField("IActivityManagerSingleton")
                } catch (e2: Exception) {
                    Log.e(e1)
                    Log.e(e2)
                    return
                }
            }
            val gDefault = fgDefault.get(null)
            val cSingleton = Class.forName("android.util.Singleton")
            val fmInstance = cSingleton.field("mInstance")
            val mInstance = fmInstance.get(gDefault)
            val proxy = Proxy.newProxyInstance(
                ActivityProxyManager.MODULE_CLASS_LOADER,
                arrayOf(Class.forName("android.app.IActivityManager")),
                IActivityManagerHandler(mInstance!!)
            )
            fmInstance.set(gDefault, proxy)
            try {
                val cActivityTaskManager = Class.forName("android.app.ActivityTaskManager")
                val singleton =
                    cActivityTaskManager.getStaticObjectOrNull("IActivityTaskManagerSingleton")
                cSingleton.method("get").invoke(singleton)
                val mDefaultTaskMgr = fmInstance.get(singleton)
                val proxy2 = Proxy.newProxyInstance(
                    ActivityProxyManager.MODULE_CLASS_LOADER,
                    arrayOf(Class.forName("android.app.IActivityTaskManager")),
                    IActivityManagerHandler(mDefaultTaskMgr!!)
                )
                fmInstance.set(singleton, proxy2)
            } catch (ignored: Exception) {
            }
            isStubHooked = true
            Log.d("ActivityProxy IActivityManager part finished")
            //endregion
            Log.i("ActivityProxy successfully inited")
        }.logexIfThrow("Activity proxy init failed!")
    }
}

class MyInstrumentation(private val mBase: Instrumentation) : Instrumentation() {

    override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
        try {
            return mBase.newActivity(cl, className, intent)
        } catch (e: Exception) {
            if (className!!.startsWith(ActivityProxyManager.MODULE_PACKAGE_NAME_ID)) {
                return ActivityHelper::class.java.classLoader!!.loadClass(className)
                    .newInstance() as Activity
            }
            throw e
        }
    }

    override fun onCreate(arguments: Bundle?) {
        mBase.onCreate(arguments)
    }

    override fun start() {
        mBase.start()
    }

    override fun onStart() {
        mBase.onStart()
    }

    override fun onException(obj: Any?, e: Throwable?): Boolean {
        return mBase.onException(obj, e)
    }

    override fun sendStatus(resultCode: Int, results: Bundle?) {
        mBase.sendStatus(resultCode, results)
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun addResults(results: Bundle?) {
        mBase.addResults(results)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        mBase.finish(resultCode, results)
    }

    override fun setAutomaticPerformanceSnapshots() {
        mBase.setAutomaticPerformanceSnapshots()
    }

    override fun startPerformanceSnapshot() {
        mBase.startPerformanceSnapshot()
    }

    override fun endPerformanceSnapshot() {
        mBase.endPerformanceSnapshot()
    }

    override fun onDestroy() {
        mBase.onDestroy()
    }

    override fun getContext(): Context {
        return mBase.context
    }

    override fun getComponentName(): ComponentName {
        return mBase.componentName
    }

    override fun getTargetContext(): Context {
        return mBase.targetContext
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun getProcessName(): String {
        return mBase.processName
    }

    override fun isProfiling(): Boolean {
        return mBase.isProfiling
    }

    override fun startProfiling() {
        mBase.startProfiling()
    }

    override fun stopProfiling() {
        mBase.stopProfiling()
    }

    override fun setInTouchMode(inTouch: Boolean) {
        mBase.setInTouchMode(inTouch)
    }

    override fun waitForIdle(recipient: Runnable?) {
        mBase.waitForIdle(recipient)
    }

    override fun waitForIdleSync() {
        mBase.waitForIdleSync()
    }

    override fun runOnMainSync(runner: Runnable?) {
        mBase.runOnMainSync(runner)
    }

    override fun startActivitySync(intent: Intent?): Activity {
        return mBase.startActivitySync(intent)
    }

    @TargetApi(Build.VERSION_CODES.P)
    override fun startActivitySync(intent: Intent, options: Bundle?): Activity {
        return mBase.startActivitySync(intent, options)
    }

    override fun addMonitor(monitor: ActivityMonitor?) {
        mBase.addMonitor(monitor)
    }

    override fun addMonitor(
        cls: String?,
        result: ActivityResult?,
        block: Boolean
    ): ActivityMonitor {
        return mBase.addMonitor(cls, result, block)
    }

    override fun addMonitor(
        filter: IntentFilter?,
        result: ActivityResult?,
        block: Boolean
    ): ActivityMonitor {
        return mBase.addMonitor(filter, result, block)
    }

    override fun checkMonitorHit(monitor: ActivityMonitor?, minHits: Int): Boolean {
        return mBase.checkMonitorHit(monitor, minHits)
    }

    override fun waitForMonitor(monitor: ActivityMonitor?): Activity {
        return mBase.waitForMonitor(monitor)
    }

    override fun waitForMonitorWithTimeout(monitor: ActivityMonitor?, timeOut: Long): Activity {
        return mBase.waitForMonitorWithTimeout(monitor, timeOut)
    }

    override fun removeMonitor(monitor: ActivityMonitor?) {
        mBase.removeMonitor(monitor)
    }

    override fun invokeContextMenuAction(
        targetActivity: Activity?,
        id: Int,
        flag: Int
    ): Boolean {
        return mBase.invokeContextMenuAction(targetActivity, id, flag)
    }

    override fun invokeMenuActionSync(targetActivity: Activity?, id: Int, flag: Int): Boolean {
        return mBase.invokeMenuActionSync(targetActivity, id, flag)
    }

    override fun sendCharacterSync(keyCode: Int) {
        mBase.sendCharacterSync(keyCode)
    }

    override fun sendKeyDownUpSync(key: Int) {
        mBase.sendKeyDownUpSync(key)
    }

    override fun sendKeySync(event: KeyEvent?) {
        mBase.sendKeySync(event)
    }

    override fun sendPointerSync(event: MotionEvent?) {
        mBase.sendPointerSync(event)
    }

    override fun sendStringSync(text: String?) {
        mBase.sendStringSync(text)
    }

    override fun sendTrackballEventSync(event: MotionEvent?) {
        mBase.sendTrackballEventSync(event)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return mBase.newApplication(cl, className, context)
    }

    override fun callApplicationOnCreate(app: Application?) {
        mBase.callApplicationOnCreate(app)
    }

    override fun newActivity(
        clazz: Class<*>?,
        context: Context?,
        token: IBinder?,
        application: Application?,
        intent: Intent?,
        info: ActivityInfo?,
        title: CharSequence?,
        parent: Activity?,
        id: String?,
        lastNonConfigurationInstance: Any?
    ): Activity {
        return mBase.newActivity(
            clazz,
            context,
            token,
            application,
            intent,
            info,
            title,
            parent,
            id,
            lastNonConfigurationInstance
        )
    }

    private fun inject(
        activity: Activity,
        icicle: Bundle?
    ) {
        val clzName = activity.javaClass.name
        if (icicle != null && clzName.startsWith(ActivityProxyManager.MODULE_PACKAGE_NAME_ID)) {
            icicle.classLoader = ActivityProxyManager.MODULE_CLASS_LOADER
        }
        if (clzName.startsWith(ActivityProxyManager.MODULE_PACKAGE_NAME_ID)) {
            activity.resources.addModuleAssetPath()
        }
    }

    override fun callActivityOnCreate(
        activity: Activity,
        icicle: Bundle?,
        persistentState: PersistableBundle?
    ) {
        inject(activity, icicle)
        mBase.callActivityOnCreate(activity, icicle, persistentState)
    }

    override fun callActivityOnCreate(activity: Activity, icicle: Bundle?) {
        inject(activity, icicle)
        mBase.callActivityOnCreate(activity, icicle)
    }

    override fun callActivityOnDestroy(activity: Activity?) {
        mBase.callActivityOnDestroy(activity)
    }

    override fun callActivityOnRestoreInstanceState(
        activity: Activity,
        savedInstanceState: Bundle
    ) {
        mBase.callActivityOnRestoreInstanceState(activity, savedInstanceState)
    }

    override fun callActivityOnRestoreInstanceState(
        activity: Activity,
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        mBase.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState)
    }

    override fun callActivityOnPostCreate(activity: Activity, savedInstanceState: Bundle?) {
        mBase.callActivityOnPostCreate(activity, savedInstanceState)
    }

    override fun callActivityOnPostCreate(
        activity: Activity,
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        mBase.callActivityOnPostCreate(activity, savedInstanceState, persistentState)
    }

    override fun callActivityOnNewIntent(activity: Activity?, intent: Intent?) {
        mBase.callActivityOnNewIntent(activity, intent)
    }

    override fun callActivityOnStart(activity: Activity?) {
        mBase.callActivityOnStart(activity)
    }

    override fun callActivityOnRestart(activity: Activity?) {
        mBase.callActivityOnRestart(activity)
    }

    override fun callActivityOnPause(activity: Activity?) {
        mBase.callActivityOnPause(activity)
    }

    override fun callActivityOnResume(activity: Activity?) {
        mBase.callActivityOnResume(activity)
    }

    override fun callActivityOnStop(activity: Activity?) {
        mBase.callActivityOnStop(activity)
    }

    override fun callActivityOnUserLeaving(activity: Activity?) {
        mBase.callActivityOnUserLeaving(activity)
    }

    override fun callActivityOnSaveInstanceState(activity: Activity, outState: Bundle) {
        mBase.callActivityOnSaveInstanceState(activity, outState)
    }

    override fun callActivityOnSaveInstanceState(
        activity: Activity,
        outState: Bundle,
        outPersistentState: PersistableBundle
    ) {
        mBase.callActivityOnSaveInstanceState(activity, outState, outPersistentState)
    }

    @TargetApi(Build.VERSION_CODES.R)
    override fun callActivityOnPictureInPictureRequested(activity: Activity) {
        mBase.callActivityOnPictureInPictureRequested(activity)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun startAllocCounting() {
        mBase.startAllocCounting()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun stopAllocCounting() {
        mBase.stopAllocCounting()
    }

    override fun getAllocCounts(): Bundle {
        return mBase.allocCounts
    }

    override fun getBinderCounts(): Bundle {
        return mBase.binderCounts
    }

    override fun getUiAutomation(): UiAutomation {
        return mBase.uiAutomation
    }

    override fun getUiAutomation(flags: Int): UiAutomation {
        return mBase.getUiAutomation(flags)
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun acquireLooperManager(looper: Looper?): TestLooperManager {
        return mBase.acquireLooperManager(looper)
    }
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
class MyHandler(private val mDefault: Handler.Callback?) : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            100 -> {
                runCatching {
                    val record = msg.obj
                    val fIntent = record::class.java.field("intent")
                    val intent = fIntent.get(record)!! as Intent
                    //获取bundle
                    var bundle: Bundle? = null
                    try {
                        val fExtras = Intent::class.java.field("mExtras")
                        bundle = fExtras.get(intent) as Bundle?
                    } catch (e: Exception) {
                        Log.e(e)
                    }
                    //设置
                    bundle?.let {
                        it.classLoader = appContext.classLoader
                        if (intent.hasExtra(ActivityProxyManager.ACTIVITY_PROXY_INTENT)) {
                            val rIntent =
                                intent.getParcelableExtra<Intent>(ActivityProxyManager.ACTIVITY_PROXY_INTENT)
                            fIntent.set(record, rIntent)
                        }
                    }
                }.logexIfThrow("ActivityProxy handle message error(what=100)")
            }
            159 -> {
                val clientTranslation = msg.obj
                runCatching {
                    clientTranslation?.let { cTrans ->
                        //获取列表
                        val mGetCallbacks =
                            Class.forName("android.app.servertransaction.ClientTransaction")
                                .method("getCallbacks")
                        val cTransItems = mGetCallbacks.invoke(cTrans) as List<*>?
                        if (!cTransItems.isNullOrEmpty()) {
                            for (item in cTransItems) {
                                val clz = item?.javaClass
                                if (clz?.name?.contains("LaunchActivityItem") == true) {
                                    val fmIntent = clz.field("mIntent")
                                    val wrapper = fmIntent.get(item) as Intent
                                    //获取Bundle
                                    var bundle: Bundle? = null
                                    try {
                                        val fExtras = Intent::class.java.field("mExtras")
                                        bundle = fExtras.get(wrapper) as Bundle?
                                    } catch (e: Exception) {
                                        Log.e(e)
                                    }
                                    //设置
                                    bundle?.let { b ->
                                        b.classLoader = ActivityProxyManager.HOST_CLASS_LOADER
                                        if (wrapper.hasExtra(ActivityProxyManager.ACTIVITY_PROXY_INTENT)) {
                                            val rIntent = wrapper.getParcelableExtra<Intent>(
                                                ActivityProxyManager.ACTIVITY_PROXY_INTENT
                                            )
                                            fmIntent.set(item, rIntent)
                                            if (Build.VERSION.SDK_INT >= 31) {
                                                val cActivityThread =
                                                    Class.forName("android.app.ActivityThread")
                                                val activityThread =
                                                    cActivityThread.staticMethod("currentActivityThread")
                                                        .invoke(null)
                                                val acr = activityThread.javaClass
                                                    .method(
                                                        "getLaunchingActivity",
                                                        argTypes = argTypes(IBinder::class.java)
                                                    )
                                                    .invoke(
                                                        activityThread,
                                                        cTrans.javaClass.method("getActivityToken").invoke(cTrans)
                                                    )
                                                acr?.let {
                                                    val fAcrIntent = it.javaClass.field("intent")
                                                    fAcrIntent[it] = rIntent
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.logexIfThrow("ActivityProxy handle message error(what=159)")
            }
        }
        return mDefault?.handleMessage(msg) ?: false
    }
}

class IActivityManagerHandler(private val mOrigin: Any) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<Any>?): Any? {
        if ("startActivity" == method!!.name) {
            val index = args?.indexOfFirst { it is Intent } ?: -1
            if (index != -1) {
                args?.let {
                    val raw = it[index] as Intent
                    val component = raw.component
                    if (component != null &&
                        appContext.packageName == component.packageName &&
                        component.className.startsWith(ActivityProxyManager.MODULE_PACKAGE_NAME_ID)
                    ) {
                        val wrapper = Intent()
                        wrapper.setClassName(
                            component.packageName,
                            ActivityProxyManager.HOST_ACTIVITY_PROXY_NAME
                        )
                        wrapper.putExtra(ActivityProxyManager.ACTIVITY_PROXY_INTENT, raw)
                        it[index] = wrapper
                    }
                }
            }
        }
        try {
            return if (args != null) method.invoke(mOrigin, *args) else method.invoke(mOrigin)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}
