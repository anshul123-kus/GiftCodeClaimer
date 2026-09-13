package com.giftclaimer.app.webview

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.R
import com.giftclaimer.app.data.AppDatabase
import com.giftclaimer.app.data.models.ClaimLog
import com.giftclaimer.app.data.models.Profile
import com.giftclaimer.app.data.models.Site
import com.giftclaimer.app.ui.MainActivity
import com.giftclaimer.app.util.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CodeSubmitter(private val context: Context) {

    private val TAG = "CodeSubmitter"
    private val db = AppDatabase.getInstance(context)
    private val handler = Handler(Looper.getMainLooper())
    private val webViews = mutableMapOf<String, WebView>()
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val notificationIdCounter = AtomicInteger(Constants.RESULT_NOTIFICATION_ID_BASE)

    suspend fun submitCodeToAllProfiles(code: String) {
        val activeSites = db.siteDao().getActiveSites()
        val activeProfiles = db.profileDao().getActiveProfiles()

        if (activeSites.isEmpty() || activeProfiles.isEmpty()) {
            Log.w(TAG, "No active sites or profiles")
            return
        }

        Log.d(TAG, "Submitting code $code to ${activeSites.size} sites x ${activeProfiles.size} profiles")

        var totalSuccess = 0
        var totalAttempts = 0
        val allLogs = mutableListOf<ClaimLog>()

        // Process profiles sequentially (cookie isolation)
        for (profile in activeProfiles) {
            // Load cookies for this profile
            withContext(Dispatchers.Main) {
                loadProfileCookies(profile)
            }

            // Submit to all sites in parallel for this profile
            val siteLogs = coroutineScope {
                activeSites.map { site ->
                    async {
                        submitToSite(site, profile, code)
                    }
                }.awaitAll()
            }

            allLogs.addAll(siteLogs)
            totalSuccess += siteLogs.count { it.result == "SUCCESS" }
            totalAttempts += siteLogs.size

            // Save cookies after submission
            withContext(Dispatchers.Main) {
                saveProfileCookies(profile)
            }

            // Small delay between profiles
            delay(300)
        }

        // Save all logs to database
        db.claimLogDao().insertAll(allLogs)

        // Show result notification
        showResultNotification(code, totalSuccess, totalAttempts)

        // Vibrate/sound on success
        if (totalSuccess > 0) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getBoolean(Constants.PREF_VIBRATE_ON_SUCCESS, true)) {
                vibrate()
            }
        }
    }

    private suspend fun submitToSite(site: Site, profile: Profile, code: String): ClaimLog {
        return withContext(Dispatchers.Main) {
            val result = CompletableDeferred<ClaimLog>()

            try {
                val webView = getOrCreateWebView("${profile.id}_${site.id}")

                // Load the site URL
                val pageLoaded = CompletableDeferred<Boolean>()
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageLoaded.complete(true)
                    }

                    override fun onReceivedError(
                        view: WebView?, request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            pageLoaded.complete(false)
                        }
                    }
                }

                // Only reload if URL is different
                val currentUrl = webView.url ?: ""
                if (!currentUrl.contains(site.url.substringBefore("#"))) {
                    webView.loadUrl(site.url)
                    val loaded = pageLoaded.await()
                    if (!loaded) {
                        result.complete(ClaimLog(
                            code = code,
                            siteName = site.name,
                            profileName = profile.name,
                            result = "FAILED",
                            message = "Page load failed"
                        ))
                        return@withContext result.await()
                    }
                    // Wait for page to fully render
                    delay(2000)
                } else {
                    delay(500)
                }

                // Step 1: Dismiss any existing popup
                val dismissJs = """
                    (function() {
                        var popup = document.querySelector('${escapeSelector(site.popupSelector)}');
                        if (popup && popup.offsetParent !== null) {
                            popup.click();
                        }
                        return 'OK';
                    })()
                """.trimIndent()
                evaluateJsAsync(webView, dismissJs)
                delay(300)

                // Step 2: Fill input and click submit
                val submitJs = """
                    (function() {
                        try {
                            var input = document.querySelector('${escapeSelector(site.inputSelector)}');
                            if (!input) return 'NO_INPUT';
                            
                            var nativeSetter = Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype, 'value').set;
                            nativeSetter.call(input, '$code');
                            input.dispatchEvent(new Event('input', {bubbles: true}));
                            input.dispatchEvent(new Event('change', {bubbles: true}));
                            input.dispatchEvent(new Event('blur', {bubbles: true}));
                            
                            var btn = null;
                            var buttons = document.querySelectorAll('button, .van-button');
                            for (var i = 0; i < buttons.length; i++) {
                                if (buttons[i].textContent.trim().toLowerCase().includes('${site.buttonSelector.lowercase()}')) {
                                    btn = buttons[i];
                                    break;
                                }
                            }
                            
                            if (!btn) return 'NO_BUTTON';
                            btn.click();
                            return 'SUBMITTED';
                        } catch(e) {
                            return 'ERROR:' + e.message;
                        }
                    })()
                """.trimIndent()

                val submitResult = evaluateJsAsync(webView, submitJs)
                Log.d(TAG, "Submit result for ${site.name}/${profile.name}: $submitResult")

                if (submitResult.contains("NO_INPUT") || submitResult.contains("NO_BUTTON")) {
                    result.complete(ClaimLog(
                        code = code,
                        siteName = site.name,
                        profileName = profile.name,
                        result = "FAILED",
                        message = submitResult.replace("\"", "")
                    ))
                    return@withContext result.await()
                }

                // Step 3: Wait and check result
                delay(1500)

                val checkJs = """
                    (function() {
                        try {
                            var popup = document.querySelector('.van-dialog');
                            if (popup && popup.style.display !== 'none') {
                                var msgEl = popup.querySelector('.van-dialog__message');
                                var msg = msgEl ? msgEl.textContent.trim() : '';
                                
                                var confirmBtn = document.querySelector('${escapeSelector(site.popupSelector)}');
                                if (confirmBtn) confirmBtn.click();
                                
                                if (msg.toLowerCase().includes('${site.successText.lowercase()}')) {
                                    return 'SUCCESS:' + msg;
                                }
                                return 'POPUP:' + msg;
                            }
                            
                            var body = document.body ? document.body.textContent : '';
                            if (body.toLowerCase().includes('${site.successText.lowercase()}')) {
                                return 'SUCCESS';
                            }
                            
                            return 'UNKNOWN';
                        } catch(e) {
                            return 'ERROR:' + e.message;
                        }
                    })()
                """.trimIndent()

                val checkResult = evaluateJsAsync(webView, checkJs)
                Log.d(TAG, "Check result for ${site.name}/${profile.name}: $checkResult")

                val cleanResult = checkResult.replace("\"", "")
                val claimResult = when {
                    cleanResult.startsWith("SUCCESS") -> "SUCCESS"
                    cleanResult.startsWith("POPUP:") -> "POPUP"
                    cleanResult.startsWith("ERROR:") -> "FAILED"
                    else -> "FAILED"
                }
                val message = cleanResult.substringAfter(":", "")

                // Step 4: Clear input for next code
                val clearJs = """
                    (function() {
                        var input = document.querySelector('${escapeSelector(site.inputSelector)}');
                        if (input) {
                            var nativeSetter = Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype, 'value').set;
                            nativeSetter.call(input, '');
                            input.dispatchEvent(new Event('input', {bubbles: true}));
                        }
                        // Dismiss any remaining popup
                        var popup = document.querySelector('${escapeSelector(site.popupSelector)}');
                        if (popup) popup.click();
                        return 'CLEARED';
                    })()
                """.trimIndent()
                evaluateJsAsync(webView, clearJs)

                result.complete(ClaimLog(
                    code = code,
                    siteName = site.name,
                    profileName = profile.name,
                    result = claimResult,
                    message = message
                ))

            } catch (e: Exception) {
                Log.e(TAG, "Error submitting to ${site.name}/${profile.name}", e)
                result.complete(ClaimLog(
                    code = code,
                    siteName = site.name,
                    profileName = profile.name,
                    result = "FAILED",
                    message = e.message ?: "Unknown error"
                ))
            }

            result.await()
        }
    }

    private suspend fun evaluateJsAsync(webView: WebView, js: String): String {
        return withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String>()
            webView.evaluateJavascript(js) { value ->
                deferred.complete(value ?: "null")
            }
            deferred.await()
        }
    }

    private fun getOrCreateWebView(key: String): WebView {
        return webViews.getOrPut(key) {
            createHiddenWebView()
        }
    }

    private fun createHiddenWebView(): WebView {
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.webChromeClient = WebChromeClient()
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // Add to window manager as invisible view
        try {
            val params = WindowManager.LayoutParams(
                1, 1,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            windowManager.addView(webView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot add WebView to WindowManager. Need SYSTEM_ALERT_WINDOW permission.", e)
        }

        return webView
    }

    private fun loadProfileCookies(profile: Profile) {
        val cookieManager = CookieManager.getInstance()
        // Load cookies from profile-specific file
        val cookieFile = File(context.filesDir, "${Constants.PROFILE_DIR_PREFIX}${profile.id}/cookies.txt")
        if (cookieFile.exists()) {
            val cookieData = cookieFile.readText()
            // Parse: each line is "url|||cookie"
            cookieData.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("|||")
                if (parts.size == 2) {
                    cookieManager.setCookie(parts[0], parts[1])
                }
            }
            cookieManager.flush()
        }
    }

    private fun saveProfileCookies(profile: Profile) {
        val cookieManager = CookieManager.getInstance()
        val dir = File(context.filesDir, "${Constants.PROFILE_DIR_PREFIX}${profile.id}")
        if (!dir.exists()) dir.mkdirs()

        val cookieFile = File(dir, "cookies.txt")
        val sb = StringBuilder()

        // Save cookies for known site URLs
        val sites = webViews.keys.filter { it.startsWith("${profile.id}_") }
        val urls = mutableSetOf<String>()

        for (key in sites) {
            val webView = webViews[key]
            val url = webView?.url ?: continue
            val domain = url.substringAfter("://").substringBefore("/")
            urls.add("https://$domain")
        }

        for (url in urls) {
            val cookies = cookieManager.getCookie(url)
            if (cookies != null) {
                cookies.split(";").forEach { cookie ->
                    sb.appendLine("$url|||${cookie.trim()}")
                }
            }
        }

        cookieFile.writeText(sb.toString())
    }

    private fun showResultNotification(code: String, success: Int, total: Int) {
        val shortCode = code.take(8) + "..."
        val title = if (success > 0) "\u2705 $success/$total Success!" else "\u274c All Failed"
        val text = "Code: $shortCode — $success/$total claimed successfully"

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GiftClaimerApp.CHANNEL_RESULTS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationIdCounter.getAndIncrement(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e)
        }
    }

    private fun escapeSelector(selector: String): String {
        return selector.replace("'", "\\'")
    }

    fun destroy() {
        webViews.values.forEach { webView ->
            try {
                windowManager.removeView(webView)
            } catch (e: Exception) { /* ignore */ }
            webView.destroy()
        }
        webViews.clear()
    }
}
