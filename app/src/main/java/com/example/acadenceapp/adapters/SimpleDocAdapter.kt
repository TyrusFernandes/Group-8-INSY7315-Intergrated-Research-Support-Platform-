package com.example.acadenceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadenceapp.R
import com.google.android.material.card.MaterialCardView

class SimpleDocAdapter(
    private var items: MutableList<Pair<String, String>> // (title, dateText)
) : RecyclerView.Adapter<SimpleDocAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.docCard)
        val title: TextView = v.findViewById(R.id.docTitle)
        val date: TextView = v.findViewById(R.id.docDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doc_chip, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (t, d) = items[position]
        holder.title.text = t
        holder.date.text = d
    }

    fun update(newItems: List<Pair<String, String>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
