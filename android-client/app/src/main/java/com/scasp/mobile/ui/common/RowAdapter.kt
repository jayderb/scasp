package com.scasp.mobile.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scasp.mobile.databinding.ItemRowBinding

/** One row: title, subtitle, optional action button. */
data class Row(val key: String, val title: String, val subtitle: String, val action: String? = null, val secondAction: String? = null)

class RowAdapter(
    private val onAction: (Row) -> Unit = {},
    private val onSecondAction: (Row) -> Unit = {},
) : ListAdapter<Row, RowAdapter.Holder>(DIFF) {
    class Holder(val b: ItemRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: Holder, position: Int) {
        val row = getItem(position)
        h.b.title.text = row.title
        h.b.subtitle.text = row.subtitle
        h.b.action.visibility = if (row.action == null) View.GONE else View.VISIBLE
        h.b.action.text = row.action
        h.b.action.contentDescription = row.action?.let { "$it ${row.title}" }
        h.b.action.setOnClickListener { onAction(row) }
        h.b.secondAction.visibility = if (row.secondAction == null) View.GONE else View.VISIBLE
        h.b.secondAction.text = row.secondAction
        h.b.secondAction.contentDescription = row.secondAction?.let { "$it ${row.title}" }
        h.b.secondAction.setOnClickListener { onSecondAction(row) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(a: Row, b: Row) = a.key == b.key
            override fun areContentsTheSame(a: Row, b: Row) = a == b
        }
    }
}
