package com.giftclaimer.app.ui.profiles

import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.databinding.ActivityProfileLoginBinding
import com.giftclaimer.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ProfileLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileLoginBinding
    private var profileId: Long = 0
    private var profileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getLongExtra("profile_id", 0)
        profileName = intent.getStringExtra("profile_name") ?: "Profile"

        binding.tvLoginTitle.text = "Login: $profileName"

        setupWebView()

        binding.btnDoneLogin.setOnClickListener {
            saveCookiesAndFinish()
        }
    }

    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        binding.webViewLogin.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            cookieManager.setAcceptThirdPartyCookies(this, true)
        }

        // Load saved cookies if they exist
        loadProfileCookies()

        // Load first active site URL
        CoroutineScope(Dispatchers.IO).launch {
            val db = (application as GiftClaimerApp).database
            val sites = db.siteDao().getActiveSites()
            val url = sites.firstOrNull()?.url ?: "https://jaiclub28.com/#/main/RedeemGift"

            runOnUiThread {
                binding.webViewLogin.loadUrl(url)
            }
        }
    }

    private fun loadProfileCookies() {
        val cookieFile = File(filesDir, "${Constants.PROFILE_DIR_PREFIX}${profileId}/cookies.txt")
        if (cookieFile.exists()) {
            val cookieManager = CookieManager.getInstance()
            cookieFile.readText().lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("|||")
                if (parts.size == 2) {
                    cookieManager.setCookie(parts[0], parts[1])
                }
            }
            cookieManager.flush()
        }
    }

    private fun saveCookiesAndFinish() {
        val cookieManager = CookieManager.getInstance()
        val url = binding.webViewLogin.url ?: return
        val domain = url.substringAfter("://").substringBefore("/")

        val cookies = cookieManager.getCookie("https://$domain")
        if (cookies != null) {
            val dir = File(filesDir, "${Constants.PROFILE_DIR_PREFIX}$profileId")
            if (!dir.exists()) dir.mkdirs()

            val cookieFile = File(dir, "cookies.txt")
            val sb = StringBuilder()
            cookies.split(";").forEach { cookie ->
                sb.appendLine("https://$domain|||${cookie.trim()}")
            }
            cookieFile.writeText(sb.toString())

            Log.d("ProfileLogin", "Cookies saved for profile $profileId")
        }

        // Mark profile as logged in
        CoroutineScope(Dispatchers.IO).launch {
            val db = (application as GiftClaimerApp).database
            val profile = db.profileDao().getById(profileId)
            if (profile != null) {
                db.profileDao().update(profile.copy(isLoggedIn = true))
            }
        }

        finish()
    }

    override fun onBackPressed() {
        if (binding.webViewLogin.canGoBack()) {
            binding.webViewLogin.goBack()
        } else {
            saveCookiesAndFinish()
        }
    }
}
