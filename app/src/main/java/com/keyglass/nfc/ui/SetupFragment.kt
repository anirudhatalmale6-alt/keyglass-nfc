package com.keyglass.nfc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.keyglass.nfc.MainActivity
import com.keyglass.nfc.R
import com.keyglass.nfc.data.AppDatabase
import com.keyglass.nfc.data.Identifier
import com.keyglass.nfc.databinding.DialogEditIdentifierBinding
import com.keyglass.nfc.databinding.FragmentSetupBinding
import com.keyglass.nfc.nfc.NfcHelper
import kotlinx.coroutines.launch

/** Tab 3 — write the Base Code to a tag and manage identifiers. */
class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val dao by lazy { AppDatabase.get(requireContext()).identifierDao() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = IdentifierAdapter(
            editable = true,
            onEdit = { showEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.isNestedScrollingEnabled = false
        binding.list.adapter = adapter
        dao.getAll().observe(viewLifecycleOwner) { adapter.submitList(it) }

        binding.btnWrite.setOnClickListener { armWrite() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }
    }

    private fun armWrite() {
        val activity = activity as? MainActivity ?: return
        val code = binding.baseCode.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) {
            binding.baseCodeLayout.error = getString(R.string.enter_base_code)
            return
        }
        binding.baseCodeLayout.error = null
        if (!activity.nfc.isAvailable) {
            Toast.makeText(requireContext(), R.string.nfc_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (!activity.nfc.isEnabled) {
            Toast.makeText(requireContext(), R.string.nfc_disabled, Toast.LENGTH_LONG).show()
            return
        }
        val protect = binding.writeProtect.isChecked
        binding.setupStatus.setText(R.string.ready_to_write)
        activity.armWrite(code, protect) { result ->
            if (!isAdded) return@armWrite
            val msg = when (result) {
                NfcHelper.WriteResult.SUCCESS -> getString(R.string.write_success)
                NfcHelper.WriteResult.NOT_WRITABLE -> getString(R.string.write_not_writable)
                NfcHelper.WriteResult.TOO_LARGE -> getString(R.string.write_too_large)
                NfcHelper.WriteResult.IO_ERROR -> getString(R.string.write_io_error)
                NfcHelper.WriteResult.FORMAT_ERROR -> getString(R.string.write_format_error)
            }
            binding.setupStatus.text = msg
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(requireContext(), R.string.hold_tag_now, Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(existing: Identifier?) {
        val dialogBinding = DialogEditIdentifierBinding.inflate(layoutInflater)
        dialogBinding.inputCode.setText(existing?.code ?: "")
        dialogBinding.inputAccount.setText(existing?.account ?: "")

        val title = if (existing == null) R.string.add_identifier else R.string.edit_identifier
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val code = dialogBinding.inputCode.text?.toString()?.trim().orEmpty().uppercase()
                val account = dialogBinding.inputAccount.text?.toString()?.trim().orEmpty()
                if (code.isEmpty() || account.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.fields_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    if (existing == null) {
                        val next = dao.count()
                        dao.insert(Identifier(code = code, account = account, position = next))
                    } else {
                        dao.update(existing.copy(code = code, account = account))
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(item: Identifier) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_identifier)
            .setMessage(getString(R.string.delete_confirm, item.code, item.account))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch { dao.delete(item) }
            }
            .show()
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.disarm()
        _binding = null
        super.onDestroyView()
    }
}
