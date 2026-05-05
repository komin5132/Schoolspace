package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class ManageScheduleFragment : Fragment(R.layout.fragment_manage_schedule) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etClass = view.findViewById<TextInputEditText>(R.id.etTargetClass)
        val etDay = view.findViewById<TextInputEditText>(R.id.etLessonDay)
        val etTime = view.findViewById<TextInputEditText>(R.id.etLessonTime)
        val etSubj = view.findViewById<TextInputEditText>(R.id.etLessonSubject)
        val btnAddPerm = view.findViewById<Button>(R.id.btnAddLessonPermanent)

        val etDate = view.findViewById<TextInputEditText>(R.id.etChangeDate)
        val etChangeSubj = view.findViewById<TextInputEditText>(R.id.etChangeSubject)
        val btnAddChange = view.findViewById<Button>(R.id.btnAddLessonChange)

        val db = FirebaseFirestore.getInstance()

        btnAddPerm.setOnClickListener {
            val className = etClass.text.toString().trim()
            val day = etDay.text.toString().trim().lowercase()
            val time = etTime.text.toString().trim()
            val subj = etSubj.text.toString().trim()

            if (className.isEmpty() || day.isEmpty()) return@setOnClickListener

            val lesson = hashMapOf("time" to time, "subject" to subj, "day" to day)
            db.collection("schedules").document(className).collection("weekly").add(lesson)
                .addOnSuccessListener { Toast.makeText(context, "Dodano do planu", Toast.LENGTH_SHORT).show() }
        }

        btnAddChange.setOnClickListener {
            val className = etClass.text.toString().trim()
            val date = etDate.text.toString().trim()
            val subj = etChangeSubj.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (className.isEmpty() || date.isEmpty()) return@setOnClickListener

            val change = hashMapOf("date" to date, "newSubject" to subj, "time" to time, "isCancelled" to subj.contains("odwoł", true))
            db.collection("schedules").document(className).collection("changes").add(change)
                .addOnSuccessListener { Toast.makeText(context, "Dodano zmianę", Toast.LENGTH_SHORT).show() }
        }
    }
}
