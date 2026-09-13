package com.giftclaimer.app.util

object Constants {
    const val PREF_CHECK_INTERVAL = "pref_check_interval"
    const val PREF_SOUND_ON_SUCCESS = "pref_sound_on_success"
    const val PREF_VIBRATE_ON_SUCCESS = "pref_vibrate_on_success"
    const val PREF_AUTO_START = "pref_auto_start"

    const val DEFAULT_CHECK_INTERVAL = 500L
    const val SERVICE_NOTIFICATION_ID = 1
    const val RESULT_NOTIFICATION_ID_BASE = 100

    const val ACTION_START_SERVICE = "com.giftclaimer.START"
    const val ACTION_STOP_SERVICE = "com.giftclaimer.STOP"
    const val ACTION_CODE_DETECTED = "com.giftclaimer.CODE_DETECTED"
    const val EXTRA_CODE = "extra_code"

    const val PROFILE_DIR_PREFIX = "webview_profile_"
}
