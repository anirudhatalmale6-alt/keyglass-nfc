package com.keyglass.nfc

import android.content.Context

/**
 * The app's few user settings. Only booleans — no Base Code or identifier value
 * is ever persisted here.
 */
object Prefs {

    private const val FILE = "loopo3fa_prefs"
    private const val KEY_MASK = "mask_base_code"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Whether the Base Code is shown partially masked on the Base Code tab. */
    fun maskBaseCode(context: Context): Boolean = prefs(context).getBoolean(KEY_MASK, true)

    fun setMaskBaseCode(context: Context, mask: Boolean) {
        prefs(context).edit().putBoolean(KEY_MASK, mask).apply()
    }
}
