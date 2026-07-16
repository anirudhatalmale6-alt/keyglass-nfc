package com.keyglass.nfc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.keyglass.nfc.data.AppDatabase
import com.keyglass.nfc.databinding.FragmentIdentifiersBinding

/** Tab 2 — read-only list of identifiers for daily reference. */
class IdentifiersFragment : Fragment() {

    private var _binding: FragmentIdentifiersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifiersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = IdentifierAdapter(editable = false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        val dao = AppDatabase.get(requireContext()).identifierDao()
        dao.getAll().observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
