package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class ManageGradesFragment : Fragment(R.layout.fragment_manage_grades) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etEmail = view.findViewById<AutoCompleteTextView>(R.id.etStudentEmail)
        val etSubject = view.findViewById<AutoCompleteTextView>(R.id.etSubject)
        val etValue = view.findViewById<EditText>(R.id.etGradeValue)
        val tilValue = view.findViewById<TextInputLayout>(R.id.tilGradeValue)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)
        val tilWeight = view.findViewById<TextInputLayout>(R.id.tilWeight)
        val etDesc = view.findViewById<EditText>(R.id.etDescription)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGradeSubmit)

        val db = FirebaseFirestore.getInstance()

        // 1. Autouzupełnianie uczniów
        val studentList = mutableListOf<String>()
        val studentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, studentList)
        etEmail.setAdapter(studentAdapter)

        db.collection("users").get().addOnSuccessListener { docs ->
            studentList.clear()
            docs.forEach { doc ->
                val email = doc.getString("email")
                val role = doc.getString("role")
                if (email != null && role == "student") studentList.add(email)
            }
            studentAdapter.notifyDataSetChanged()
        }

        // 2. Dozwolone oceny i przedmioty
        val allowedGrades = listOf("1", "1+", "2-", "2", "2+", "3-", "3", "3+", "4-", "4", "4+", "5-", "5", "5+", "6-", "6")
        val subjects = arrayOf("Język polski", "Język angielski", "Matematyka", "Fizyka", "Chemia", "Biologia", "Geografia", "Historia", "Informatyka", "WF")
        etSubject.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, subjects))

        btnAdd.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val subject = etSubject.text.toString().trim()
            val value = etValue.text.toString().trim().replace(" ", "")
            val weightStr = etWeight.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (email.isEmpty() || subject.isEmpty() || value.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(context, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!allowedGrades.contains(value)) {
                tilValue.error = "Błędna ocena!"
                return@setOnClickListener
            } else {
                tilValue.error = null
            }

            val weight = weightStr.toIntOrNull()
            if (weight == null || weight !in 1..5) {
                tilWeight.error = "Waga 1-5!"
                return@setOnClickListener
            } else {
                tilWeight.error = null
            }

            db.collection("users").whereEqualTo("email", email).whereEqualTo("role", "student").get().addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val grade = hashMapOf(
                        "subject" to subject,
                        "value" to value,
                        "weight" to weight,
                        "description" to desc,
                        "date" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("users").document(docs.documents[0].id).collection("grades").add(grade)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Dodano: $value (waga $weight)", Toast.LENGTH_SHORT).show()
                            etValue.setText(""); etDesc.setText(""); etWeight.setText("1")
                        }
                } else {
                    Toast.makeText(context, "Nie znaleziono ucznia o podanym emailu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
