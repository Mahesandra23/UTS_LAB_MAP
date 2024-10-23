package com.example.uts_lab_map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameEditText: EditText
    private lateinit var nimEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var editProfileButton: Button
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        bottomNavigationView = view.findViewById(R.id.bottom_navigation)
        nameEditText = view.findViewById(R.id.profile_name)
        nimEditText = view.findViewById(R.id.profile_nim)
        emailEditText = view.findViewById(R.id.profile_email)
        editProfileButton = view.findViewById(R.id.edit_profile_button)

        // Load user profile data
        loadUserProfile()

        bottomNavigationView.selectedItemId = R.id.nav_profile

        // Set up BottomNavigationView with NavController
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    findNavController().navigate(R.id.homeFragment)
                    true
                }
                R.id.nav_history -> {
                    findNavController().navigate(R.id.historyFragment)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        // Handle button click for editing or saving profile
        editProfileButton.setOnClickListener {
            if (isEditing) {
                if (validateInputs()) {
                    saveUserProfile()
                }
            } else {
                enableEditing()
            }
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name") ?: ""
                        val nim = document.getString("nim") ?: ""
                        val email = document.getString("email") ?: ""

                        nameEditText.setText(name)
                        nimEditText.setText(nim)
                        emailEditText.setText(email)

                        // Ensure email is always disabled
                        emailEditText.isEnabled = false
                    }
                }
        }
    }

    private fun enableEditing() {
        // Enable EditTexts except for email
        nameEditText.isEnabled = true
        nimEditText.isEnabled = true

        // Change button text to "Save Edit"
        editProfileButton.text = "Save Edit"
        isEditing = true
    }

    private fun disableEditing() {
        // Disable EditTexts
        nameEditText.isEnabled = false
        nimEditText.isEnabled = false
        emailEditText.isEnabled = false // Keep email disabled

        // Change button text back to "Edit Profile"
        editProfileButton.text = "Edit Profile"
        isEditing = false
    }

    private fun validateInputs(): Boolean {
        val name = nameEditText.text.toString().trim()
        val nim = nimEditText.text.toString().trim()

        // Check if NIM is at least 5 digits
        if (nim.length < 5) {
            Toast.makeText(context, "NIM must be at least 5 digits", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid
        val name = nameEditText.text.toString().trim()
        val nim = nimEditText.text.toString().trim()

        if (userId != null) {
            val userUpdates = hashMapOf(
                "name" to name,
                "nim" to nim
                // Email is not included as it cannot be edited
            )

            firestore.collection("users").document(userId).update(userUpdates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    disableEditing()
                    findNavController().navigate(R.id.profileFragment) // Navigate back to home
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
