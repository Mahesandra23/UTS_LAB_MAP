package com.example.uts_lab_map

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SignUpFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var fstore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        val signUpButton = view.findViewById<Button>(R.id.btn_signup)
        val namaEditText = view.findViewById<EditText>(R.id.nama)
        val emailEditText = view.findViewById<EditText>(R.id.et_email_signup)
        val passwordEditText = view.findViewById<EditText>(R.id.et_password_signup)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.et_confirm_password_signup)
        val nimEditText = view.findViewById<EditText>(R.id.nim)

        auth = FirebaseAuth.getInstance()
        fstore = FirebaseFirestore.getInstance()

        // Fungsi untuk mengecek apakah ada input yang kosong atau tidak valid
        fun isFormValid(): Boolean {
            return namaEditText.error == null && !namaEditText.text.isEmpty() &&
                    nimEditText.error == null && !namaEditText.text.isEmpty() &&
                    emailEditText.error == null && !emailEditText.text.isEmpty() &&
                    passwordEditText.error == null && !passwordEditText.text.isEmpty() &&
                    confirmPasswordEditText.error == null && !confirmPasswordEditText.text.isEmpty()
        }

        // Fungsi untuk mengatur warna button berdasarkan validitas form
        fun updateSignUpButtonState() {
            if (isFormValid()) {
                signUpButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                signUpButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                signUpButton.isEnabled = true
            } else {
                signUpButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey))
                signUpButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                signUpButton.isEnabled = false
            }
        }

        // Tambahkan TextWatcher untuk memantau perubahan input
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSignUpButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        namaEditText.addTextChangedListener(textWatcher)
        nimEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)

        // Set initial state of button
        updateSignUpButtonState()

        // Handle ketika tombol Sign Up diklik
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            val nim = nimEditText.text.toString()

            if (password == confirmPassword) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid

                            // Simpan data pengguna ke Firebase Firestore
                            val userMap = hashMapOf(
                                "name" to namaEditText.text.toString(),
                                "nim" to nimEditText.text.toString(),
                                "email" to email
                            )

                            userId?.let {
                                // Simpan data di collection "users" dengan document ID adalah userId
                                fstore.collection("users").document(it).set(userMap)
                                    .addOnCompleteListener { firestoreTask ->
                                        if (firestoreTask.isSuccessful) {
                                            Toast.makeText(activity, "Sign Up successful", Toast.LENGTH_SHORT).show()
                                            // Pindah ke halaman login setelah signup berhasil
                                            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
                                        } else {
                                            Toast.makeText(activity, "Firestore Error: ${firestoreTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(activity, "Sign Up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(activity, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }



        val loginTextView: TextView = view.findViewById(R.id.tv_login)
        loginTextView.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        // Validasi Email
        fun isEmailValid(email: String): Boolean {
            return email.contains("@")
        }

        // Validasi No. Telepon
        fun isPhoneNumberValid(phone: String): Boolean {
            return phone.length >= 10 && phone.all { it.isDigit() }
        }

        // Validasi Password
        fun isPasswordValid(password: String): Boolean {
            return password.length >= 7
        }

        // Validasi NIM - Harus minimal 5 angka
        nimEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length ?: 0 < 5) {
                    nimEditText.error = "NIM harus minimal 5 angka"
                } else {
                    nimEditText.error = null
                }
            }
        })

        // Menambahkan TextWatcher untuk validasi langsung
        namaEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    namaEditText.error = "Name cannot be empty"
                } else {
                    namaEditText.error = null
                }
            }
        })

        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isEmailValid(s.toString())) {
                    emailEditText.error = "Email must contain '@'"
                } else {
                    emailEditText.error = null
                }
            }
        })


        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isPasswordValid(s.toString())) {
                    passwordEditText.error = "Password must be at least 7 characters"
                } else {
                    passwordEditText.error = null
                }
            }
        })

        confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != passwordEditText.text.toString()) {
                    confirmPasswordEditText.error = "Passwords do not match"
                } else {
                    confirmPasswordEditText.error = null
                }
            }
        })

        return view
    }
}
