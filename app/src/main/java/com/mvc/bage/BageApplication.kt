package com.mvc.bage

import android.app.Activity
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.TextUtils
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.chat.advanced.BageAdvancedApplication
import com.chat.base.BageBaseApplication
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageConfig
import com.chat.base.config.BageConstants
import com.chat.base.config.BageSharedPreferencesUtil
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.net.RetrofitUtils
import com.chat.base.ui.Theme
import com.chat.base.utils.ActManagerUtils
import com.chat.base.utils.BagePlaySound
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.language.BageMultiLanguageUtil
import com.chat.file.BageFileApplication
import com.chat.groupmanage.BageGroupManageApplication
import com.chat.imgeditor.BageImageEditorApplication
import com.chat.login.BageLoginApplication
import com.chat.moments.BageMomentsApplication
import com.chat.push.BagePushApplication
import com.chat.scan.BageScanApplication
import com.chat.sticker.BageStickerApplication
import com.chat.uikit.TabActivity
import com.chat.uikit.BageUIKitApplication
import com.chat.uikit.chat.manager.BageIMUtils
import com.chat.uikit.user.service.UserModel
import com.chat.video.BageVideoApplication
import com.tencent.bugly.crashreport.CrashReport
import kotlin.system.exitProcess

class BageApplication : MultiDexApplication() {
    val KEY_API_URL = "api_url"
    val DEFAULT_API_URL = "http://api.newhxchat.top/api"
    val BUGLY_ID = "d383347352"

    // 使用单例模式保存实例引用
    companion object {
        private lateinit var instance: BageApplication

        fun getInstance(): BageApplication {
            return instance
        }
    }
    // 标记API组件是否已初始化
    private var isApiInitialized = false

    override fun onCreate() {
        super.onCreate()

        instance = this  // 保存实例引用

        // 优先初始化Bugly，确保异常处理器在CrashHandler之前设置
        CrashReport.initCrashReport(applicationContext, BUGLY_ID, false)
        
        val processName = getProcessName(this, Process.myPid())
        if (processName != null) {
            val defaultProcess = processName == getAppPackageName()
            if (defaultProcess) {
                initBasicComponents()
                val cachedApiUrl = BageSharedPreferencesUtil.getInstance().getSP(KEY_API_URL)
                val apiUrl = if (cachedApiUrl.isNullOrBlank()) DEFAULT_API_URL else cachedApiUrl
                val needShowAgreement = BageSharedPreferencesUtil.getInstance()
                    .getBoolean("show_agreement_dialog")

                // Android 可能在后台回收进程却保留 Activity 任务栈。此时回到应用
                // 会直接恢复 TabActivity，不会先经过 SplashActivity，因此必须在
                // Application.onCreate() 中恢复 IM、数据库和业务监听器。
                if (!needShowAgreement) {
                    initApiDependentComponents(apiUrl)
                } else {
                    // 隐私协议同意前不初始化业务模块，但保证协议页 URL 可用。
                    initApi(apiUrl)
                }
            }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            }

            override fun onActivityStarted(p0: Activity) {
            }

            override fun onActivityResumed(p0: Activity) {
                ActManagerUtils.getInstance().currentActivity = p0
            }

            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
            }
        })
    }

    fun ensureInitialized() {
        if (!isApiInitialized) {
            // 重新初始化必要组件
            initBasicComponents()
            // 获取保存的API URL
            val cachedApiUrl = BageSharedPreferencesUtil.getInstance().getSP(KEY_API_URL)
            val apiUrl = if (cachedApiUrl.isNullOrBlank()) DEFAULT_API_URL else cachedApiUrl
            initApiDependentComponents(apiUrl)
        }
    }



    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        BageMultiLanguageUtil.getInstance().setConfiguration()
        Theme.applyTheme()
        if (Theme.isForceLightTheme()) {
            return
        }
        if (applicationContext != null && applicationContext.resources != null && applicationContext.resources.configuration != null && applicationContext.resources.configuration.uiMode != newConfig.uiMode) {
            killAppProcess()
        }
    }

    private fun killAppProcess() {
        ActManagerUtils.getInstance().clearAllActivity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(BageMultiLanguageUtil.getInstance().attachBaseContext(base))
    }

//    private fun initAll() {
//
//        BageMultiLanguageUtil.getInstance().init(this)
//        BageBaseApplication.getInstance().init(getAppPackageName(), this)
//        Theme.applyTheme()
//        initApi()
//        BageLoginApplication.getInstance().init(this)
//        BageScanApplication.getInstance().init(this)
//        BageUIKitApplication.getInstance().init(this)
//        BagePushApplication.getInstance().init(getAppPackageName(), this)
//        addAppFrontBack()
//        addListener()
//    }

    fun initBasicComponents(){
        BageMultiLanguageUtil.getInstance().init(this)
        BageBaseApplication.getInstance().init(getAppPackageName(), this)
        Theme.applyTheme()
        // 其他不依赖API的基础初始化...
    }

    fun initApiDependentComponents(apiUrl: String) {
        // 避免重复初始化
        if (isApiInitialized) return

        // 清理可能存在的重复菜单项
        EndpointManager.getInstance().clearCategory(EndpointCategory.personalCenter)
        EndpointManager.getInstance().clearCategory(EndpointCategory.mailList)
        EndpointManager.getInstance().clearCategory(EndpointCategory.chatFunction)
        EndpointManager.getInstance().clearCategory(EndpointCategory.tabMenus)

        // 保存API地址到本地缓存，下次启动可以直接使用
//        BageSharedPreferencesUtil.getInstance().putSP(KEY_API_URL, apiUrl)

        // 初始化API
        initApi(apiUrl)

        // 初始化其他依赖API的组件，确保使用正确的上下文
        BageLoginApplication.getInstance().init(this) // 这里的this始终指向BageApplication实例
        BageScanApplication.getInstance().init(this)
        BageUIKitApplication.getInstance().init(this)
        BagePushApplication.getInstance().init(getAppPackageName(), this)
        BageGroupManageApplication.getInstance().init()
        BageFileApplication.getInstance().init(this)
        BageVideoApplication.getInstance().init(this)
        BageMomentsApplication.getInstance().init(this)
        BageAdvancedApplication.instance.init()
        BageImageEditorApplication.getInstance().init()
        UserModel.getInstance().getOnlineUsers()
        BageStickerApplication.instance.init()
        // 添加其他监听器
        addAppFrontBack()
        addListener()

        isApiInitialized = true
    }

    /**
     * 应用从 Splash 获取到远程配置后统一从这里应用。
     *
     * Application 为了兼容进程被系统回收后直接恢复 Activity，会先使用缓存地址
     * 初始化业务组件。Splash 随后仍会拉取远程配置；如果地址发生变化，需要同时
     * 更新 BageApiConfig 并丢弃绑定了旧 baseUrl 的 Retrofit 实例。
     */
    @Synchronized
    fun applyRemoteApiUrl(apiUrl: String) {
        val normalizedApiUrl = apiUrl.trim().trimEnd('/')
        require(
            normalizedApiUrl.startsWith("http://") ||
                normalizedApiUrl.startsWith("https://")
        ) { "无效的 API 地址" }

        if (!isApiInitialized) {
            initApiDependentComponents(normalizedApiUrl)
            return
        }

        val expectedBaseUrl = "$normalizedApiUrl/v1/"
        if (BageApiConfig.baseUrl != expectedBaseUrl) {
            initApi(normalizedApiUrl)
            RetrofitUtils.getInstance().resetRetrofit()
            Log.i("RemoteConfig", "已应用新的远程 API 配置")
        }
    }


    private fun initApi() {
        Log.d("BageApplication", "初始化了 ")
        var apiURL = BageSharedPreferencesUtil.getInstance().getSP("api_base_url")
        if (TextUtils.isEmpty(apiURL)) {
            apiURL = "http://api.newhxchat.top/api"
            BageApiConfig.initBaseURL(apiURL)
        } else {
            BageApiConfig.initBaseURLIncludeIP(apiURL)
        }
    }


    private fun initApi(apiUrl: String) {
        Log.d("BageApplication 初始化", "当前初始化API地址  $apiUrl")
        BageApiConfig.initBaseURL(apiUrl)
        // 其他API相关设置...
    }

    // 检查API组件是否已初始化
    fun isApiInitialized(): Boolean {
        return isApiInitialized
    }

    // 重置API初始化状态（用于重试场景）
    fun resetApiInitialized() {
        isApiInitialized = false
    }


    private fun getAppPackageName(): String {
        return "com.mvc.bage"
    }

    private fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (app in runningApps) {
            if (app.pid == pid) {
                return app.processName
            }
        }
        return null
    }

    private fun addAppFrontBack() {
        val helper = AppFrontBackHelper()
        helper.register(this, object : AppFrontBackHelper.OnAppStatusListener {
            override fun onFront() {
                if (!TextUtils.isEmpty(BageConfig.getInstance().token)) {
                    if (BageBaseApplication.getInstance().disconnect) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            EndpointManager.getInstance()
                                .invoke("chow_check_lock_screen_pwd", null)
                        }, 1000)
                    }
                    // connection() 本身是幂等的。回到前台时始终校验连接，不能用相册/相机使用的
                    // disconnect 标志代替 IM 的真实连接状态。
                    BageIMUtils.getInstance().initIMListener()
                    BageUIKitApplication.getInstance().startChat()
                    UserModel.getInstance().getOnlineUsers()

                }
            }

            override fun onBack() {
                // IM 由前台服务在后台继续保活，这里不主动断开连接。
                BageSharedPreferencesUtil.getInstance()
                    .putLong("lock_start_time", BageTimeUtils.getInstance().currentSeconds)

            }
        })
    }

    private fun addListener() {
        createNotificationChannel()
        EndpointManager.getInstance().setMethod("main_show_home_view") { `object` ->
            if (`object` != null) {
                val from = `object` as Int
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("from", from)
                startActivity(intent)
            }
            null
        }
        EndpointManager.getInstance().setMethod("show_tab_home") {
            val intent = Intent(applicationContext, TabActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            null
        }

        EndpointManager.getInstance().setMethod("play_new_msg_Media") {
            BagePlaySound.getInstance().playRecordMsg(R.raw.newmsg)
            null
        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = applicationContext.getString(R.string.new_msg_notification)
            val description = applicationContext.getString(R.string.new_msg_notification_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(BageConstants.newMsgChannelID, name, importance)
            channel.description = description
            channel.enableVibration(true) //是否有震动
            channel.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newmsg),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
        createNotificationRTCChannel()
    }

    private fun createNotificationRTCChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = applicationContext.getString(R.string.new_rtc_notification)
            val description = applicationContext.getString(R.string.new_rtc_notification_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(BageConstants.newRTCChannelID, name, importance)
            channel.description = description
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 100, 100, 100, 100, 100)
            channel.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newrtc),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

}
