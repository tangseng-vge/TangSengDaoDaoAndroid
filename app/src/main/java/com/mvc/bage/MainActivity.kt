package com.mvc.bage

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.chat.base.BageBaseApplication
import com.chat.base.base.BageBaseActivity
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageConfig
import com.chat.base.config.BageSharedPreferencesUtil
import com.chat.base.ui.Theme
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.systembar.BageStatusBarUtils
import com.chat.login.ui.PerfectUserInfoActivity
import com.chat.login.ui.BageLoginActivity
import com.chat.uikit.TabActivity
import com.mvc.bage.databinding.ActivityMainBinding
import com.bage.im.BageIM

class MainActivity : BageBaseActivity<ActivityMainBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AppFlow", "[MainActivity] onCreate called")

        // 检查应用是否已正确初始化
        if (!BageApplication.getInstance().isApiInitialized()) {
            Log.d("AppFlow", "[MainActivity] API not initialized, redirecting to SplashActivity")
            // 如果未初始化，重定向到SplashActivity
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            return
        }
    }


    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        Log.d("AppFlow", "[MainActivity] initView called")
        // BageBaseActivity 会在 MainActivity.onCreate() 返回前调用 initView()。
        // 未初始化时由 onCreate() 统一跳转 SplashActivity，避免提前进入空数据页。
        if (!BageApplication.getInstance().isApiInitialized()) {
            return
        }
        val isShowDialog: Boolean =
            BageSharedPreferencesUtil.getInstance().getBoolean("show_agreement_dialog")
        Log.d("AppFlow", "[MainActivity] show_agreement_dialog: $isShowDialog")
        if (isShowDialog) {
            Log.d("AppFlow", "[MainActivity] Redirecting to SplashActivity for agreement")
            // 将隐私协议放到 SplashActivity 统一处理，避免跳过 getConfig
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            return
        } else {
            Log.d("AppFlow", "[MainActivity] Calling gotoApp")
            gotoApp()
        }
    }

    private fun gotoApp() {
        Log.d("AppFlow", "[MainActivity] gotoApp called")
        val token = BageConfig.getInstance().token
        Log.d("AppFlow", "[MainActivity] Token: ${if (token.isNullOrEmpty()) "empty" else "exists"}")
        
        if (!TextUtils.isEmpty(token)) {
            val userInfo = BageConfig.getInstance().userInfo
            Log.d("AppFlow", "[MainActivity] User name: ${if (userInfo.name.isNullOrEmpty()) "empty" else "exists"}")
            
            if (TextUtils.isEmpty(userInfo.name)) {
                Log.d("AppFlow", "[MainActivity] Starting PerfectUserInfoActivity")
                startActivity(Intent(this@MainActivity, PerfectUserInfoActivity::class.java))
            } else {
                val publicRSAKey: String = BageIM.getInstance().cmdManager.rsaPublicKey
                Log.d("AppFlow", "[MainActivity] RSA Key: ${if (publicRSAKey.isNullOrEmpty()) "empty" else "exists"}")
                
                if (TextUtils.isEmpty(publicRSAKey)) {
                    Log.d("AppFlow", "[MainActivity] Starting BageLoginActivity (no RSA key)")
                    val intent = Intent(this@MainActivity, BageLoginActivity::class.java)
                    intent.putExtra("from", getIntent().getIntExtra("from", 0))
                    startActivity(intent)
                } else {
                    Log.d("AppFlow", "[MainActivity] Starting TabActivity")
                    startActivity(Intent(this@MainActivity, TabActivity::class.java))
                }
            }
        } else {
            Log.d("AppFlow", "[MainActivity] Starting BageLoginActivity (no token)")
            val intent = Intent(this@MainActivity, BageLoginActivity::class.java)
            intent.putExtra("from", getIntent().getIntExtra("from", 0))
            startActivity(intent)
        }
        Log.d("AppFlow", "[MainActivity] Finishing MainActivity")
        finish()
    }

    private fun showDialog() {
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
                // 协议在 MainActivity 不再处理，交给 SplashActivity 继续后续流程
                val intent = Intent(this, SplashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }
    }
}
