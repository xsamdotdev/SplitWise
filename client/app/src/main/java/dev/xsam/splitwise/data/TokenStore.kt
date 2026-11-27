package dev.xsam.splitwise.data

import android.content.Context

class TokenStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val token: String?
        get() = preferences.getString(KEY_TOKEN, null)

    val username: String?
        get() = preferences.getString(KEY_USERNAME, null)

    fun save(token: String, username: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "splitwise_auth"
        const val KEY_TOKEN = "token"
        const val KEY_USERNAME = "username"
    }
}
