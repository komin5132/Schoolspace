package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class ManageGradesFragment : Fragment(R.layout.fragment_manage_grades) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etEmail = view.findViewById<TextInputEditText>(R.id.etStudentEmail)
        val etSubject = view.findViewById<TextInputEditText>(R.id.etSubject)
        val etValue = view.findViewById<TextInputEditText>(R.id.etGradeValue)
        val etDesc = view.findViewById<TextInputEditText>(R.id.etDescription)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGradeSubmit)

        btnAdd.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val subject = etSubject.text.toString().trim()
            val value = etValue.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (email.isEmpty() || subject.isEmpty() || value.isEmpty()) return@setOnClickListener

            val db = FirebaseFirestore.getInstance()
            db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val uid = docs.documents[0].id
                        val grade = hashMapOf(
                            "subject" to subject,
                            "value" to value,
                            "description" to desc,
                            "date" to com.google.firebase.Timestamp.now()
                        )
                        db.collection("users").document(uid).collection("grades").add(grade)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Dodano ocenę", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }
    }
}
