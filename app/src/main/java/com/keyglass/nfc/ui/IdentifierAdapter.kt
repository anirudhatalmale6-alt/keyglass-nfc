package com.keyglass.nfc.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keyglass.nfc.data.Identifier
import com.keyglass.nfc.databinding.ItemIdentifierBinding

/**
 * Shared adapter for both the read-only Identifiers tab and the editable Setup
 * list. Edit/delete icons are shown only when [editable] is true.
 */
class IdentifierAdapter(
    private val editable: Boolean,
    private val onEdit: (Identifier) -> Unit = {},
    private val onDelete: (Identifier) -> Unit = {}
) : ListAdapter<Identifier, IdentifierAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemIdentifierBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemIdentifierBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Identifier) {
            b.code.text = item.code
            b.account.text = item.account
            b.btnEdit.visibility = if (editable) android.view.View.VISIBLE else android.view.View.GONE
            b.btnDelete.visibility = if (editable) android.view.View.VISIBLE else android.view.View.GONE
            b.btnEdit.setOnClickListener { onEdit(item) }
            b.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Identifier>() {
            override fun areItemsTheSame(old: Identifier, new: Identifier) = old.id == new.id
            override fun areContentsTheSame(old: Identifier, new: Identifier) = old == new
        }
    }
}
