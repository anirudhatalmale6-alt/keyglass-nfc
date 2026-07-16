package com.keyglass.nfc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.keyglass.nfc.databinding.ActivityMainBinding
import com.keyglass.nfc.nfc.NfcHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var nfc: NfcHelper
        private set

    /** What to do with the next scanned tag. Set by the active tab. */
    private var pending: Pending? = null

    private sealed class Pending {
        data class Read(val onResult: (String?) -> Unit) : Pending()
        data class Write(
            val text: String,
            val writeProtect: Boolean,
            val onResult: (NfcHelper.WriteResult) -> Unit
        ) : Pending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfc = NfcHelper(this)

        val adapter = com.keyglass.nfc.ui.PagerAdapter(this)
        binding.pager.adapter = adapter
        binding.pager.isUserInputEnabled = true

        val titles = arrayOf(
            getString(R.string.tab_base_code),
            getString(R.string.tab_identifiers),
            getString(R.string.tab_setup)
        )
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        binding.tabs.tabGravity = TabLayout.GRAVITY_FILL
    }

    override fun onResume() {
        super.onResume()
        nfc.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfc.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = nfc.extractTag(intent) ?: return
        when (val p = pending) {
            is Pending.Read -> {
                pending = null
                val text = nfc.readText(tag)
                vibrate()
                p.onResult(text)
            }
            is Pending.Write -> {
                pending = null
                val result = nfc.writeText(tag, p.text, p.writeProtect)
                vibrate()
                p.onResult(result)
            }
            null -> { /* No tab is waiting for a tag; ignore. */ }
        }
    }

    fun armRead(onResult: (String?) -> Unit) {
        pending = Pending.Read(onResult)
    }

    fun armWrite(text: String, writeProtect: Boolean, onResult: (NfcHelper.WriteResult) -> Unit) {
        pending = Pending.Write(text, writeProtect, onResult)
    }

    fun disarm() {
        pending = null
    }

    val isArmed: Boolean get() = pending != null

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }
}
