package com.example.uts_lab_map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.UUID
import java.util.TimerTask

class HomeFragment : Fragment() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var currentDateTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var greetingTextView: TextView
    private lateinit var attendanceCircle: View
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var currentState = AttendanceState.IDLE // Attendance state
    private var lastEntryCaptureTime: Long = 0 // Timestamp for the last entry
    private var lastExitCaptureTime: Long = 0 // Timestamp for the last exit

    private val REQUEST_CAMERA_PERMISSION = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002

    // Define AttendanceState enum once
    enum class AttendanceState {
        IDLE, ENTRY_CAPTURED, EXIT_CAPTURED, WAITING_FOR_NEW_ENTRY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Firebase auth, firestore, and storage
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Get references to TextViews
        currentDateTextView = view.findViewById(R.id.current_date)
        currentTimeTextView = view.findViewById(R.id.current_time)
        greetingTextView = view.findViewById(R.id.greeting_name)

        // Reference to the attendance circle
        attendanceCircle = view.findViewById(R.id.circle)

        // Set click listener for the attendance circle
        attendanceCircle.setOnClickListener {
            // Check for camera permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            } else {
                // Launch camera
                launchCamera()
            }
        }

        // Initialize the date and time
        updateTime()

        // Fetch user details and set greeting
        fetchUserDetails()

        // Set up BottomNavigationView
        bottomNavigationView = view.findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    Toast.makeText(context, "ini history", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.historyFragment)
                    true
                }
                R.id.nav_profile -> {
                    findNavController().navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        return view
    }

    // Method to launch the camera
    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera() // Permission granted, launch camera
            } else {
                Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // In the onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val userId = auth.currentUser?.uid

            val currentTime = System.currentTimeMillis()

            when (currentState) {
                AttendanceState.IDLE -> {
                    // Entry image captured
                    Toast.makeText(context, "Entry image captured", Toast.LENGTH_SHORT).show()
                    uploadImageToFirebase(userId, imageBitmap, timestamp, true)
                    currentState = AttendanceState.ENTRY_CAPTURED
                    lastEntryCaptureTime = currentTime
                }
                AttendanceState.ENTRY_CAPTURED -> {
                    // Exit image captured
                    Toast.makeText(context, "Exit image captured", Toast.LENGTH_SHORT).show()
                    uploadImageToFirebase(userId, imageBitmap, timestamp, false)
                    currentState = AttendanceState.EXIT_CAPTURED
                    lastExitCaptureTime = currentTime
                    // Transition to WAITING_FOR_NEW_ENTRY state
                    currentState = AttendanceState.WAITING_FOR_NEW_ENTRY
                    // Start a timer to return to IDLE state after 1 minute
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentState = AttendanceState.IDLE
                    }, 60000) // 1 minute in milliseconds
                }
                AttendanceState.WAITING_FOR_NEW_ENTRY -> {
                    // Check if 1 minute has passed for a new entry capture
                    if (currentTime - lastExitCaptureTime < 60000) {
                        Toast.makeText(context, "Please wait 1 minute before taking new entry attendance.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    // If it's allowed, set back to IDLE
                    currentState = AttendanceState.IDLE
                    lastEntryCaptureTime = currentTime // Update lastEntryCaptureTime after successful entry capture
                }
                AttendanceState.EXIT_CAPTURED -> {
                    // Handle case where EXIT_CAPTURED is reached
                    Toast.makeText(context, "Already captured exit image. Please take a new entry image.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // This acts as a catch-all for any unexpected states
                    Log.e("AttendanceState", "Unexpected state: $currentState")
                    currentState = AttendanceState.IDLE // Reset to IDLE state for safety
                }
            }
        }
    }


    private fun uploadImageToFirebase(userId: String?, imageBitmap: Bitmap, timestamp: String, isEntry: Boolean) {
        if (userId != null) {
            Log.d("FirebaseAuth", "User ID: $userId") // Log User ID
            val imageName = "attendance_images/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(imageName)
            Log.d("FirebaseStorage", "Uploading to: $imageName")

            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            storageRef.putBytes(data)
                .addOnSuccessListener {
                    Log.d("FirebaseStorage", "Upload successful")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveAttendanceToFirestore(userId, uri.toString(), timestamp, isEntry)
                    }.addOnFailureListener { e ->
                        Log.e("FirebaseStorage", "Failed to get download URL: ${e.message}")
                        Toast.makeText(context, "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Image upload failed: ${e.message}")
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("FirebaseAuth", "User ID is null") // Log jika User ID null
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAttendanceToFirestore(userId: String, imageUrl: String, timestamp: String, isEntry: Boolean) {
        val attendanceDocRef = firestore.collection("attendance").document(userId)

        firestore.runTransaction { transaction ->
            val attendanceDocument = transaction.get(attendanceDocRef)

            if (attendanceDocument.exists()) {
                // Update existing attendance document
                if (isEntry) {
                    transaction.update(attendanceDocRef,
                        "entryImages", FieldValue.arrayUnion(imageUrl),
                        "entryTimestamps", FieldValue.arrayUnion(timestamp)
                    )
                } else {
                    transaction.update(attendanceDocRef,
                        "exitImages", FieldValue.arrayUnion(imageUrl),
                        "exitTimestamps", FieldValue.arrayUnion(timestamp)
                    )
                }
            } else {
                // Create a new attendance document
                val attendanceData = hashMapOf(
                    "userId" to userId,
                    "entryImages" to if (isEntry) mutableListOf(imageUrl) else mutableListOf(),
                    "entryTimestamps" to if (isEntry) mutableListOf(timestamp) else mutableListOf(),
                    "exitImages" to if (!isEntry) mutableListOf(imageUrl) else mutableListOf(),
                    "exitTimestamps" to if (!isEntry) mutableListOf(timestamp) else mutableListOf()
                )
                transaction.set(attendanceDocRef, attendanceData)
            }
        }.addOnSuccessListener {
            Log.d("Firestore", "Attendance saved successfully")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Failed to save attendance: ${e.message}")
            Toast.makeText(context, "Failed to save attendance: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetchUserDetails() {
        val user = auth.currentUser
        val userId = user?.uid

        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Ambil nama lengkap dari Firestore
                        val fullName = document.getString("name") ?: "User"

                        // Pisahkan nama menjadi list kata
                        val nameParts = fullName.split(" ")

                        // Ambil dua kata pertama (jika ada)
                        val nameToDisplay = if (nameParts.size >= 2) {
                            "${nameParts[0]} ${nameParts[1]}"
                        } else {
                            nameParts[0] // Jika hanya ada satu kata
                        }

                        // Set greeting dengan dua kata pertama
                        greetingTextView.text = "Hello, $nameToDisplay!"
                    } else {
                        greetingTextView.text = "Hello, User!"
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            greetingTextView.text = "Hello, User!"
        }
    }




    private fun updateTime() {
        // Get current date and time
        val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

        // Set the date and time
        currentDateTextView.text = currentDate
        currentTimeTextView.text = currentTime
    }

    private var timer: Timer? = null

    override fun onStart() {
        super.onStart()
        // Start a timer that updates every second
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Update UI on the main thread
                activity?.runOnUiThread {
                    updateTime()
                }
            }
        }, 0, 1000) // Update every second
    }

    override fun onStop() {
        super.onStop()
        // Stop the timer when the fragment is no longer visible
        timer?.cancel()
        timer = null
    }



}
