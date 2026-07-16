package com.keyglass.nfc.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import java.io.IOException
import java.nio.charset.Charset

/**
 * Thin wrapper around the Android NFC foreground-dispatch APIs and NDEF text
 * read/write. Callers enable dispatch in onResume and disable it in onPause,
 * then hand any NFC intent to [readTextFromIntent] or [writeText].
 */
class NfcHelper(private val activity: Activity) {

    val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isAvailable: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    fun enableForegroundDispatch() {
        val adapter = adapter ?: return
        val intent = Intent(activity, activity.javaClass)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pending = PendingIntent.getActivity(activity, 0, intent, flags)
        adapter.enableForegroundDispatch(activity, pending, null, null)
    }

    fun disableForegroundDispatch() {
        adapter?.disableForegroundDispatch(activity)
    }

    fun extractTag(intent: Intent): Tag? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    /** Reads the first NDEF text record from the tag carried by [intent]. */
    fun readTextFromIntent(intent: Intent): String? {
        val tag = extractTag(intent) ?: return null
        return readText(tag)
    }

    fun readText(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
            message?.records?.firstNotNullOfOrNull { decodeTextRecord(it) }
        } catch (e: IOException) {
            null
        } catch (e: FormatException) {
            null
        } finally {
            runCatching { ndef.close() }
        }
    }

    /**
     * Writes [text] as an NDEF text record. If [writeProtect] is true the tag is
     * permanently locked (irreversible) after a successful write.
     */
    fun writeText(tag: Tag, text: String, writeProtect: Boolean): WriteResult {
        val record = buildTextRecord(text)
        val message = NdefMessage(arrayOf(record))

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) return WriteResult.NOT_WRITABLE
                if (message.toByteArray().size > ndef.maxSize) return WriteResult.TOO_LARGE
                ndef.writeNdefMessage(message)
                if (writeProtect && ndef.canMakeReadOnly()) {
                    ndef.makeReadOnly()
                }
                WriteResult.SUCCESS
            } catch (e: IOException) {
                WriteResult.IO_ERROR
            } catch (e: FormatException) {
                WriteResult.FORMAT_ERROR
            } finally {
                runCatching { ndef.close() }
            }
        }

        // Tag not yet NDEF formatted — try to format and write in one shot.
        val formatable = NdefFormatable.get(tag) ?: return WriteResult.NOT_WRITABLE
        return try {
            formatable.connect()
            if (writeProtect) {
                formatable.formatReadOnly(message)
            } else {
                formatable.format(message)
            }
            WriteResult.SUCCESS
        } catch (e: IOException) {
            WriteResult.IO_ERROR
        } catch (e: FormatException) {
            WriteResult.FORMAT_ERROR
        } finally {
            runCatching { formatable.close() }
        }
    }

    private fun buildTextRecord(text: String, locale: String = "en"): NdefRecord {
        val langBytes = locale.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val status = langBytes.size // UTF-8, so high bit stays 0
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = status.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }

    private fun decodeTextRecord(record: NdefRecord): String? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN) return null
        if (!record.type.contentEquals(NdefRecord.RTD_TEXT)) return null
        return try {
            val payload = record.payload
            if (payload.isEmpty()) return null
            val status = payload[0].toInt()
            val langLength = status and 0x3F
            val isUtf16 = (status and 0x80) != 0
            val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
            String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
        } catch (e: Exception) {
            null
        }
    }

    enum class WriteResult {
        SUCCESS, NOT_WRITABLE, TOO_LARGE, IO_ERROR, FORMAT_ERROR
    }
}
