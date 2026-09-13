package com.giftclaimer.app.util

object CodeDetector {
    private val HEX_CODE_REGEX = Regex("[0-9A-Fa-f]{32}")

    fun findCodes(text: String): List<String> {
        return HEX_CODE_REGEX.findAll(text)
            .map { it.value.uppercase() }
            .distinct()
            .toList()
    }

    fun isValidCode(text: String): Boolean {
        return text.trim().let {
            it.length == 32 && HEX_CODE_REGEX.matches(it)
        }
    }
}
