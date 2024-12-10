package com.rsvi.sensationtask3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BenchmarkResultsAdapter(
    private val results: List<BenchmarkResult>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View types for header and item
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    // ViewHolder for the header
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerModelName: TextView = view.findViewById(R.id.headerModelName)
        val headerProcessingTime: TextView = view.findViewById(R.id.headerProcessingTime)
        val headerFps: TextView = view.findViewById(R.id.headerFps)
        val headerTotalTime: TextView = view.findViewById(R.id.headerTotalTime)
    }

    // ViewHolder for the items
    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val modelName: TextView = view.findViewById(R.id.modelName)
        val processingTime: TextView = view.findViewById(R.id.processingTime)
        val fps: TextView = view.findViewById(R.id.fps)
        val totalTime: TextView = view.findViewById(R.id.totalTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            // Inflate the header layout
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.benchmark_result_header, parent, false)
            HeaderViewHolder(view)
        } else {
            // Inflate the item layout
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.benchmark_result_item, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            // Bind data to the header (static text in this case)
            holder.headerModelName.text = "Model Name"
            holder.headerProcessingTime.text = "Processing Time"
            holder.headerFps.text = "FPS"
            holder.headerTotalTime.text = "Total Time"
        } else if (holder is ItemViewHolder) {
            // Bind data to the item
            val result = results[position - 1] // Adjust for header
            holder.modelName.text = result.modelName
            holder.processingTime.text = result.processingTime
            holder.fps.text = result.fps
            holder.totalTime.text = result.totalTime
        }
    }

    override fun getItemCount(): Int {
        return results.size + 1 // +1 for the header
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }
}
