package com.mvc.bage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chat.base.BageBaseApplication
import com.chat.base.act.BageWebViewActivity
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageSharedPreferencesUtil
import com.chat.base.ui.components.AlertDialog
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.utils.IpSearch
import com.chat.base.utils.JiamiUtil
import com.chat.base.utils.BageDialogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


@SuppressLint("CustomSplashScreen")
public final class SplashActivity : AppCompatActivity() {
    val KEY_API_URL = "api_url"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 确保全屏
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            val window: Window = getWindow()
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.setStatusBarColor(Color.TRANSPARENT)
//
//
//            // 设置内容延伸到状态栏
//            window.getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
//                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//            )
//        }


        setContentView(R.layout.splash_activity_with_animation)

        // 先检查隐私协议是否同意，未同意则先拦截，避免进入主页或跳过 getConfig
        val needShowAgreement = BageSharedPreferencesUtil.getInstance().getBoolean("show_agreement_dialog")
        if (needShowAgreement) {
            showAgreementAndThenProceed()
        } else {
            proceedInit()
        }
    }

    private fun proceedInit() {
        val bageApp = BageApplication.getInstance()

        // 正常经过 Splash 的启动始终刷新远程配置。Application 中的提前初始化
        // 只用于系统回收进程后直接恢复 Activity 时使用本地缓存兜底。
        if (isNetworkAvailable(this)) {
            getConfigAsync()
        } else if (bageApp.isApiInitialized()) {
            // 离线时继续使用 Application 已恢复的缓存地址。
            startMainActivity()
        } else {
            bageApp.initApiDependentComponents(bageApp.DEFAULT_API_URL)
            showErrorAndRetryOption()
        }
    }

    private fun showAgreementAndThenProceed() {
        val content = getString(R.string.dialog_content)
        val linkSpan = SpannableStringBuilder()
        linkSpan.append(content)
        val userAgreementIndex = content.indexOf(getString(R.string.main_user_agreement))
        linkSpan.setSpan(
            NormalClickableSpan(
                true,
                ContextCompat.getColor(this, R.color.blue),
                NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                object : NormalClickableSpan.IClick {
                    override fun onClick(view: View) {
                        showWebView(
                            BageApiConfig.baseWebUrl + "user_agreement.html"
                        )
                    }
                }), userAgreementIndex, userAgreementIndex + 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val privacyPolicyIndex = content.indexOf(getString(R.string.main_privacy_policy))
        linkSpan.setSpan(
            NormalClickableSpan(true,
                ContextCompat.getColor(this, R.color.blue),
                NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                object : NormalClickableSpan.IClick {
                    override fun onClick(view: View) {
                        showWebView(
                            BageApiConfig.baseWebUrl + "privacy_policy.html"
                        )
                    }
                }), privacyPolicyIndex, privacyPolicyIndex + 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        BageDialogUtils.getInstance().showDialog(
            this,
            getString(R.string.dialog_title),
            linkSpan,
            false,
            getString(R.string.disagree),
            getString(R.string.agree),
            0,
            0
        ) { index ->
            if (index == 1) {
                BageSharedPreferencesUtil.getInstance()
                    .putBoolean("show_agreement_dialog", false)
                BageBaseApplication.getInstance().init(
                    BageBaseApplication.getInstance().packageName,
                    BageBaseApplication.getInstance().application
                )
                proceedInit()
            } else {
                finish()
            }
        }
    }

    private fun showWebView(url: String) {
        val intent = Intent(this, BageWebViewActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
    }

    fun getConfigAsync() {
        lifecycleScope.launch {
            try {
                val apiUrl = withContext(Dispatchers.IO) {
                    val ossUrl = "https://clean-nengyuan.oss-accelerate.aliyuncs.com/config1.json"
                    Log.i("RemoteConfig", "开始获取启动配置")
                    getConfig(ossUrl)
                }

                BageApplication.getInstance().applyRemoteApiUrl(apiUrl)
                BageSharedPreferencesUtil.getInstance().putSP(KEY_API_URL, apiUrl)
                Log.i("RemoteConfig", "启动配置获取并应用成功")

                // 进入主界面
                startMainActivity()
            } catch (e: Exception) {
                val bageApp = BageApplication.getInstance()
                Log.e("RemoteConfig", "启动配置获取失败，使用本地配置", e)
                if (bageApp.isApiInitialized()) {
                    startMainActivity()
                } else {
                    bageApp.initApiDependentComponents(bageApp.DEFAULT_API_URL)
                    showErrorAndRetryOption()
                }
            }
        }
    }


    private fun startMainActivity() {
        Log.d("AppFlow", "[SplashActivity] startMainActivity called")
        // 创建进入MainActivity的Intent，让MainActivity处理隐私协议
        val intent = Intent(this, MainActivity::class.java)

        // 添加标志以清除任务栈，确保SplashActivity完全退出
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // 启动MainActivity
        startActivity(intent)

        // 结束SplashActivity
        finish()
    }

    private fun getConfig(getUrl: String): String {
        if (!isNetworkAvailable(BageBaseApplication.getInstance().getContext())) {
            throw IOException("网络连接不可用")
        }

        // 时间戳和 no-cache 请求头同时避免 HttpURLConnection、代理或 OSS 边缘节点
        // 返回上一次启动缓存的配置对象。
        val separator = if (getUrl.contains('?')) '&' else '?'
        val requestUrl = "$getUrl${separator}_t=${System.currentTimeMillis()}"
        val url = URL(requestUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache, no-store")
            connection.setRequestProperty("Pragma", "no-cache")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("获取启动配置失败，HTTP $responseCode")
            }

            val configJson = BufferedReader(
                InputStreamReader(connection.inputStream, Charsets.UTF_8)
            ).use { it.readText() }
            if (configJson.isBlank()) {
                throw IOException("启动配置内容为空")
            }

            val jsonObject = JSONObject(configJson)
            val domesticApiUrl = JiamiUtil.decrypt(jsonObject.optString("config", ""))
            val globalApiUrl = JiamiUtil.decrypt(jsonObject.optString("configJw", ""))
            val apiList = JiamiUtil.decrypt(jsonObject.optString("apiList", ""))

            val ip = getDeviceIp(apiList)
            val instance = IpSearch.getInstance(BageBaseApplication.getInstance().getContext())
            val selectedApiUrl = if (instance.getArea(ip) != "CN") {
                globalApiUrl
            } else {
                domesticApiUrl
            }.trim().trimEnd('/')

            if (!selectedApiUrl.startsWith("http://") &&
                !selectedApiUrl.startsWith("https://")
            ) {
                throw IOException("启动配置中的 API 地址无效")
            }
            return selectedApiUrl
        } finally {
            connection.disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 使用NetworkCapabilities API (推荐用于API 23及以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        // 兼容老版本Android (API 22及以下)
        else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }


    private fun getDeviceIp(ipapi: String): String {
        var ipApis: Array<String> = arrayOf<String>()

        // 优先使用从OSS返回的API列表
        if (ipapi != null && !ipapi.isEmpty()) {
            val split = ipapi.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size > 0) {
                ipApis = split
                Log.d("BageApplication", "使用OSS返回的API列表，共" + ipApis.size + "个API")
            }
        }
        if(ipApis.size == 0){
            ipApis = listOf(
                // 中国境内可用的API（优先）
                "https://whois.pconline.com.cn/ipJson.jsp?json=true",  // 太平洋IP查询

                // 国际稳定API（备用）
                "https://ifconfig.me/ip",  // ifconfig.me
                "https://icanhazip.com/",  // icanhazip
                "https://api.ipify.org?format=text",  // ipify
                "https://ipinfo.io/ip",  // ipinfo
                "https://checkip.amazonaws.com",  // AWS IP查询
                "https://api.ip.sb/ip",  // IP.SB
                "https://myip.dnsomatic.com",  // DNS-O-Matic
                "https://ipecho.net/plain"  // ipecho.net
            ).toTypedArray()
        }


        for (api in ipApis) {
            try {
                val url = URL(api)
                val connection = if (api.startsWith("https")) {
                    url.openConnection() as HttpsURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }
                connection.connectTimeout = 5000 // 设置连接超时为 3000 毫秒
                connection.readTimeout = 5000 // 设置读取超时为 3000 毫秒
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText().trim()
                    reader.close()

                    val ip = parseIpFromResponse(api, response)
                    if (isValidIpAddress(ip)) {
                        Log.d("BageApplication", "成功从 $api 获取到 IP: $ip")
                        return ip
                    } else {
                        Log.w("BageApplication", "从 $api 获取的响应不是有效IP: $response")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("BageApplication", "从 $api 获取 IP 失败: ${e.message}")
                // 继续尝试下一个 API
            }
        }

        Log.e("BageApplication", "无法从任何 API 获取 IP 地址")
        return "" // 如果所有 API 都失败，返回空字符串
    }

    /**
     * 解析不同API的响应格式，提取IP地址
     */
    private fun parseIpFromResponse(api: String, response: String): String {
        return try {
            Log.d("BageApplication", "解析API $api 的响应: $response")
            
            // 首先尝试直接提取IP地址（适用于大多数API）
            val ipPattern = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b".toRegex()
            val match = ipPattern.find(response)
            
            if (match != null) {
                val ip = match.value
                if (isValidIpAddress(ip)) {
                    return ip
                }
            }
            
            // 针对特定API的解析
            when {
                // 太平洋API - JSON格式
                api.contains("pconline.com.cn") -> {
                    try {
                        val jsonObject = JSONObject(response)
                        val ip = jsonObject.getString("ip")
                        Log.d("BageApplication", "太平洋API解析结果: $ip")
                        ip
                    } catch (e: Exception) {
                        Log.w("BageApplication", "太平洋API JSON解析失败: ${e.message}")
                        ""
                    }
                }
                
                // 检查是否包含HTML标签（说明返回了HTML页面）
                response.contains("<html", ignoreCase = true) || 
                response.contains("<!DOCTYPE", ignoreCase = true) -> {
                    Log.w("BageApplication", "API $api 返回了HTML页面而不是IP地址")
                    ""
                }
                
                // 检查是否是JSON格式
                response.startsWith("{") && response.endsWith("}") -> {
                    try {
                        val jsonObject = JSONObject(response)
                        // 尝试常见的JSON字段
                        val possibleFields = listOf("ip", "query", "origin", "client_ip")
                        for (field in possibleFields) {
                            if (jsonObject.has(field)) {
                                val ip = jsonObject.getString(field)
                                if (isValidIpAddress(ip)) {
                                    Log.d("BageApplication", "JSON API解析结果: $ip")
                                    return ip
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("BageApplication", "解析JSON失败: ${e.message}")
                    }
                    ""
                }
                
                // 默认情况：直接返回响应内容
                else -> {
                    val result = response.trim()
                    Log.d("BageApplication", "默认解析结果: $result")
                    result
                }
            }
        } catch (e: Exception) {
            Log.e("BageApplication", "解析API响应失败: ${e.message}")
            ""
        }
    }

    /**
     * 验证IP地址格式是否正确
     */
    private fun isValidIpAddress(ip: String): Boolean {
        if (ip.isBlank()) return false
        
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        for (part in parts) {
            try {
                val num = part.toInt()
                if (num < 0 || num > 255) return false
            } catch (e: NumberFormatException) {
                return false
            }
        }
        
        // 过滤掉无效的IP地址
        val invalidIps = listOf(
            "127.0.0.1",      // 本地回环地址
            "0.0.0.0",        // 无效地址
            "255.255.255.255", // 广播地址
            "169.254.0.0",    // 链路本地地址
            "192.168.0.0",    // 私有地址
            "10.0.0.0",       // 私有地址
            "172.16.0.0"      // 私有地址
        )
        
        // 检查是否是无效IP
        if (invalidIps.contains(ip)) {
            Log.w("BageApplication", "检测到无效IP地址: $ip")
            return false
        }
        
        // 检查是否是私有地址段
        val firstOctet = parts[0].toInt()
        val secondOctet = parts[1].toInt()
        
        when {
            // 10.0.0.0/8
            firstOctet == 10 -> {
                Log.w("BageApplication", "检测到私有地址段 10.x.x.x: $ip")
                return false
            }
            // 172.16.0.0/12
            firstOctet == 172 && secondOctet in 16..31 -> {
                Log.w("BageApplication", "检测到私有地址段 172.16-31.x.x: $ip")
                return false
            }
            // 192.168.0.0/16
            firstOctet == 192 && secondOctet == 168 -> {
                Log.w("BageApplication", "检测到私有地址段 192.168.x.x: $ip")
                return false
            }
        }
        
        return true
    }

    private fun showErrorAndRetryOption() {
        // 显示错误对话框，提供重试按钮
        AlertDialog.Builder(this)
            .setTitle("网络错误")
            .setMessage("无法获取API配置，请检查网络连接后重试")
            .setPositiveButton("重试") { _, _ ->
                getConfigAsync()
            }
            .setNegativeButton("退出") { _, _ -> finish() }
            .show()
    }
}
