package com.example.uts_lab_map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AttendanceAdapter(private val attendances: List<Attendance>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = attendances[position]

        // Set Entry Timestamp and Image
        holder.textEntryTimestamp.text = attendance.entryTimestamps.joinToString(", ")
        Glide.with(holder.imageEntry.context)
            .load(attendance.entryImages.firstOrNull())
            .placeholder(R.drawable.placeholder) // Ganti dengan drawable placeholder
            .into(holder.imageEntry)

        // Set Exit Timestamp and Image
        holder.textExitTimestamp.text = attendance.exitTimestamps.joinToString(", ")
        Glide.with(holder.imageExit.context)
            .load(attendance.exitImages.firstOrNull())
            .placeholder(R.drawable.placeholder) // Ganti dengan drawable placeholder
            .into(holder.imageExit)
    }

    override fun getItemCount(): Int {
        return attendances.size
    }

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textEntryTimestamp: TextView = itemView.findViewById(R.id.text_entry_timestamp)
        val imageEntry: ImageView = itemView.findViewById(R.id.image_entry)
        val textExitTimestamp: TextView = itemView.findViewById(R.id.text_exit_timestamp)
        val imageExit: ImageView = itemView.findViewById(R.id.image_exit)
    }
}
