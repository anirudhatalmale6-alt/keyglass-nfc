package com.keyglass.nfc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.keyglass.nfc.MainActivity
import com.keyglass.nfc.R
import com.keyglass.nfc.SecureClipboard
import com.keyglass.nfc.databinding.FragmentBaseCodeBinding

/**
 * Tab 1 — reads an NFC tag, shows the Base Code (as in the mockup) and holds it
 * in the secure in-app clipboard. The value stays in this app's memory only and
 * is never written to the Android system clipboard. It auto-clears after 30s.
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

        binding.versionLabel.text = "KEY GLASS v" + com.keyglass.nfc.BuildConfig.VERSION_NAME
        binding.btnRead.setOnClickListener { armRead() }
        binding.btnClear.setOnClickListener {
            SecureClipboard.clear()
            Toast.makeText(requireContext(), R.string.clipboard_cleared, Toast.LENGTH_SHORT).show()
        }
        SecureClipboard.addListener(clipboardListener)
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
                SecureClipboard.copyCode(text)
                Toast.makeText(
                    requireContext(), R.string.base_code_copied, Toast.LENGTH_SHORT
                ).show()
            }
        }
        Toast.makeText(requireContext(), R.string.hold_tag_now, Toast.LENGTH_SHORT).show()
    }

    /** Reflects the current clipboard state in the UI. */
    private fun render(held: Boolean) {
        if (held) {
            binding.codeDisplay.text = maskCode(SecureClipboard.peek())
            binding.codeDisplay.visibility = View.VISIBLE
            binding.placeholder.visibility = View.GONE
            binding.status.setText(R.string.code_secured)
        } else {
            binding.codeDisplay.text = ""
            binding.codeDisplay.visibility = View.GONE
            binding.placeholder.visibility = View.VISIBLE
            binding.status.setText(R.string.ready_prompt)
        }
    }

    /**
     * Masks the middle of the code for shoulder-surfing safety, keeping the
     * first 3 and last 2 characters visible, e.g. "G7$kL2@qZp" -> "G7$•••••Zp".
     * The full value is unaffected in the secure clipboard.
     */
    private fun maskCode(code: String?): String {
        if (code.isNullOrEmpty()) return ""
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
