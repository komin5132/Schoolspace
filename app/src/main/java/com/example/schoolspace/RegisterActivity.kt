package com.example.schoolspace

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val email = findViewById<TextInputEditText>(R.id.etEmail)
        val password = findViewById<TextInputEditText>(R.id.etPassword)
        val register = findViewById<Button>(R.id.btnRegister)
        val login = findViewById<TextView>(R.id.txtLogin)

        login.setOnClickListener {
            finish()
        }

        register.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isNotEmpty() && passwordText.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val uid = user?.uid
                            
                            // Tworzymy dane użytkownika w Firestore
                            val userMap = hashMapOf(
                                "uid" to uid,
                                "email" to emailText,
                                "role" to "unassigned", // Domyślna rola: bez przydziału
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            if (uid != null) {
                                db.collection("users").document(uid).set(userMap)
                                    .addOnSuccessListener {
                                        user.sendEmailVerification()
                                            ?.addOnCompleteListener { verifyTask ->
                                                if (verifyTask.isSuccessful) {
                                                    Toast.makeText(baseContext, 
                                                        "Rejestracja udana! Sprawdź e-mail, aby aktywować konto.", 
                                                        Toast.LENGTH_LONG).show()
                                                    auth.signOut()
                                                    finish()
                                                }
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Błąd zapisu danych: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(baseContext, "Błąd: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Pola nie mogą być puste", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
