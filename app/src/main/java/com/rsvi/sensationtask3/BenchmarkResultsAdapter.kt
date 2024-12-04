package com.rsvi.sensationtask3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BenchmarkResultsAdapter(
    private val results: List<BenchmarkResult>
) : RecyclerView.Adapter<BenchmarkResultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val modelName: TextView = view.findViewById(R.id.modelName)
        val processingTime: TextView = view.findViewById(R.id.processingTime)
        val fps: TextView = view.findViewById(R.id.fps)
        val totalTime: TextView = view.findViewById(R.id.totalTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.benchmark_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.modelName.text = result.modelName
        holder.processingTime.text = result.processingTime
        holder.fps.text = result.fps
        holder.totalTime.text = result.totalTime
    }

    override fun getItemCount(): Int = results.size
}
