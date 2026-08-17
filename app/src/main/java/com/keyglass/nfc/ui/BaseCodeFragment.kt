package com.keyglass.nfc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.keyglass.nfc.MainActivity
import com.keyglass.nfc.Prefs
import com.keyglass.nfc.R
import com.keyglass.nfc.SecureClipboard
import com.keyglass.nfc.databinding.FragmentBaseCodeBinding

/**
 * Tab 1 — reads an NFC TAG and shows the Base Code in the frame (masked, if the
 * Setup option is on). The user taps COPY to put the full code on the clipboard
 * and CLEAR CLIPBOARD when finished; nothing clears itself automatically.
 */
class BaseCodeFragment : Fragment() {

    private var _binding: FragmentBaseCodeBinding? = null
    private val binding get() = _binding!!

    private val clipboardListener: (Boolean) -> Unit = { held ->
        _binding?.let { render(held) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.versionLabel.text =
            getString(R.string.app_title) + " v" + com.keyglass.nfc.BuildConfig.VERSION_NAME
        binding.btnRead.setOnClickListener { armRead() }
        binding.btnCopy.setOnClickListener { copyCode() }
        binding.btnClear.setOnClickListener {
            SecureClipboard.clear(requireContext())
            Toast.makeText(requireContext(), R.string.clipboard_cleared, Toast.LENGTH_SHORT).show()
        }
        SecureClipboard.addListener(clipboardListener)
    }

    override fun onResume() {
        super.onResume()
        // The masking option can have been changed on the Setup tab meanwhile.
        _binding?.let { render(SecureClipboard.hasCode()) }
    }

    private fun copyCode() {
        val copied = SecureClipboard.copyToSystemClipboard(requireContext())
        val msg = if (copied) R.string.base_code_copied else R.string.nothing_to_copy
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun armRead() {
        val activity = activity as? MainActivity ?: return
        if (!activity.nfc.isAvailable) {
            Toast.makeText(requireContext(), R.string.nfc_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (!activity.nfc.isEnabled) {
            Toast.makeText(requireContext(), R.string.nfc_disabled, Toast.LENGTH_LONG).show()
            return
        }
        binding.status.setText(R.string.ready_to_read)
        activity.armRead { text ->
            if (!isAdded || _binding == null) return@armRead
            if (text.isNullOrEmpty()) {
                binding.status.setText(R.string.read_failed)
                Toast.makeText(requireContext(), R.string.read_failed, Toast.LENGTH_SHORT).show()
            } else {
                // Holding the code fires the listener, which renders it on screen.
                SecureClipboard.hold(text)
            }
        }
        Toast.makeText(requireContext(), R.string.hold_tag_now, Toast.LENGTH_SHORT).show()
    }

    /** Reflects the current state in the UI. */
    private fun render(held: Boolean) {
        if (held) {
            val code = SecureClipboard.peek().orEmpty()
            binding.codeDisplay.text =
                if (Prefs.maskBaseCode(requireContext())) maskCode(code) else code
            binding.codeDisplay.visibility = View.VISIBLE
            binding.status.setText(R.string.base_code_read)
            binding.copyHint.visibility = View.VISIBLE
        } else {
            binding.codeDisplay.text = ""
            binding.codeDisplay.visibility = View.GONE
            binding.status.setText(R.string.ready_prompt)
            binding.copyHint.visibility = View.INVISIBLE
        }
    }

    /**
     * Masks the middle of the code, keeping the first 3 and last 2 characters
     * visible, e.g. "7hD$4#sk@68GZ" -> "7hD********GZ". What gets copied to the
     * clipboard is always the full code.
     */
    private fun maskCode(code: String): String {
        if (code.isEmpty()) return ""
        val n = code.length
        if (n <= 5) {
            return if (n <= 1) code else code.take(1) + MASK.repeat(n - 1)
        }
        val prefix = 3
        val suffix = 2
        return code.take(prefix) + MASK.repeat(n - prefix - suffix) + code.takeLast(suffix)
    }

    private companion object {
        // Plain ASCII so it renders on every device/OEM font.
        const val MASK = "*"
    }

    override fun onDestroyView() {
        SecureClipboard.removeListener(clipboardListener)
        (activity as? MainActivity)?.disarm()
        _binding = null
        super.onDestroyView()
    }
}
