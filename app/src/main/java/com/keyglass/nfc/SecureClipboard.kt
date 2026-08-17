package com.keyglass.nfc

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Holds the Base Code that was just read from the NFC TAG.
 *
 * The value lives in this app's process memory. It reaches the Android system
 * clipboard only when the user taps COPY, so it can be pasted into whatever
 * field they need — and it stays there until the user taps CLEAR CLIPBOARD.
 * There is deliberately no automatic timeout: pasting into several fields in a
 * row used to break when the clipboard cleared itself mid-way.
 *
 * On Android 13+ the clip is flagged as sensitive, so the system does not show
 * the code in the paste preview toast and keeps it out of clipboard history.
 */
object SecureClipboard {

    private val handler = Handler(Looper.getMainLooper())
    private var content: String? = null

    /** Listeners are notified whenever the held/cleared state changes. */
    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    /** Remembers a freshly read Base Code. Does not touch the system clipboard. */
    @Synchronized
    fun hold(code: String) {
        content = code
        notifyState()
    }

    /** Copies the held Base Code to the system clipboard. Returns false if nothing is held. */
    @Synchronized
    fun copyToSystemClipboard(context: Context): Boolean {
        val code = content ?: return false
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        val clip = ClipData.newPlainText("", code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        manager.setPrimaryClip(clip)
        return true
    }

    /** Clears the system clipboard and forgets the held Base Code. */
    @Synchronized
    fun clear(context: Context) {
        content = null
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.clearPrimaryClip()
            } else {
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
        notifyState()
    }

    @Synchronized
    fun hasCode(): Boolean = content != null

    @Synchronized
    fun peek(): String? = content

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        listener(hasCode())
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyState() {
        val held = content != null
        handler.post { listeners.toList().forEach { it(held) } }
    }
}
