package com.example.uts_lab_map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.RecyclerView


class HistoryFragment : Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private var attendanceList = mutableListOf<Attendance>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        bottomNavigationView = view.findViewById(R.id.bottom_navigation)
        recyclerView = view.findViewById(R.id.recycler_view) // Ganti dengan id RecyclerView Anda
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Load user attendance history
        loadUserHistory()

        bottomNavigationView.selectedItemId = R.id.nav_history

        // Set up BottomNavigationView with NavController
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_history -> true
                R.id.nav_profile -> {
                    findNavController().navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        return view
    }

    private fun loadUserHistory() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("attendance").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val entryImages = document.get("entryImages") as? List<String> ?: emptyList()
                        val entryTimestamps = document.get("entryTimestamps") as? List<String> ?: emptyList()
                        val exitImages = document.get("exitImages") as? List<String> ?: emptyList()
                        val exitTimestamps = document.get("exitTimestamps") as? List<String> ?: emptyList()

                        for (i in entryImages.indices) {
                            if (i < entryTimestamps.size && i < exitImages.size && i < exitTimestamps.size) {
                                val attendance = Attendance(
                                    listOf(entryImages[i]),
                                    listOf(entryTimestamps[i]),
                                    listOf(exitImages[i]),
                                    listOf(exitTimestamps[i])
                                )
                                attendanceList.add(attendance)
                            }
                        }

                        Log.d("HistoryFragment", "Attendance list size: ${attendanceList.size}")
                        attendanceAdapter = AttendanceAdapter(attendanceList)
                        recyclerView.adapter = attendanceAdapter
                    }
                }
        }
    }
}
