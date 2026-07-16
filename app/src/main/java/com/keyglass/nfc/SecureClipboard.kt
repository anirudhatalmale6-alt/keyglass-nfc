package com.keyglass.nfc

import android.os.Handler
import android.os.Looper

/**
 * A secure, in-app clipboard for the Base Code.
 *
 * The value is held only in this app's process memory. It is deliberately NEVER
 * written to the Android system clipboard (ClipboardManager), so no other app can
 * read it. The value auto-clears after [AUTO_CLEAR_MS] and can be cleared manually.
 */
object SecureClipboard {

    const val AUTO_CLEAR_MS = 30_000L

    private val handler = Handler(Looper.getMainLooper())
    private var content: String? = null
    private var clearRunnable: Runnable? = null

    /** Listeners are notified whenever the held/cleared state changes. */
    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    @Synchronized
    fun copyCode(code: String) {
        content = code
        cancelPending()
        clearRunnable = Runnable { clear() }
        handler.postDelayed(clearRunnable!!, AUTO_CLEAR_MS)
        notifyState()
    }

    @Synchronized
    fun clear() {
        content = null
        cancelPending()
        notifyState()
    }

    @Synchronized
    fun hasCode(): Boolean = content != null

    /** Intentionally used only internally; the code is never surfaced to the UI. */
    @Synchronized
    fun peek(): String? = content

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        listener(hasCode())
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun cancelPending() {
        clearRunnable?.let { handler.removeCallbacks(it) }
        clearRunnable = null
    }

    private fun notifyState() {
        val held = content != null
        handler.post { listeners.toList().forEach { it(held) } }
    }
}
