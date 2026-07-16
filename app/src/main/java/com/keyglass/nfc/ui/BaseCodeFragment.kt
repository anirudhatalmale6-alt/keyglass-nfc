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
 * Tab 1 — reads an NFC tag and copies the Base Code into the secure in-app
 * clipboard. The code itself is never shown on screen.
 */
class BaseCodeFragment : Fragment() {

    private var _binding: FragmentBaseCodeBinding? = null
    private val binding get() = _binding!!

    private val clipboardListener: (Boolean) -> Unit = { held ->
        _binding?.let { updateStatus(held) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            if (!isAdded) return@armRead
            if (text.isNullOrEmpty()) {
                binding.status.setText(R.string.read_failed)
                Toast.makeText(requireContext(), R.string.read_failed, Toast.LENGTH_SHORT).show()
            } else {
                SecureClipboard.copyCode(text)
                Toast.makeText(
                    requireContext(), R.string.base_code_copied, Toast.LENGTH_SHORT
                ).show()
            }
        }
        Toast.makeText(requireContext(), R.string.hold_tag_now, Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(held: Boolean) {
        binding.status.setText(if (held) R.string.code_secured else R.string.ready_prompt)
    }

    override fun onDestroyView() {
        SecureClipboard.removeListener(clipboardListener)
        (activity as? MainActivity)?.disarm()
        _binding = null
        super.onDestroyView()
    }
}
