package com.example.schoolspace

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ManageScheduleFragment : Fragment(R.layout.fragment_manage_schedule) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etClass = view.findViewById<EditText>(R.id.etTargetClass)
        val etDay = view.findViewById<AutoCompleteTextView>(R.id.etLessonDay)
        val etTime = view.findViewById<AutoCompleteTextView>(R.id.etLessonTime)
        val etRoom = view.findViewById<AutoCompleteTextView>(R.id.etLessonRoom)
        val etSubj = view.findViewById<EditText>(R.id.etLessonSubject)
        val btnAddPerm = view.findViewById<Button>(R.id.btnAddLessonPermanent)

        val etDate = view.findViewById<EditText>(R.id.etChangeDate)
        val etChangeSubj = view.findViewById<EditText>(R.id.etChangeSubject)
        val btnAddChange = view.findViewById<Button>(R.id.btnAddLessonChange)

        val db = FirebaseFirestore.getInstance()

        // 1. Dni tygodnia
        val days = arrayOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek")
        etDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        // 2. Godziny lekcyjne
        val lessonHours = arrayOf(
            "08:00 - 08:45", "08:55 - 09:40", "09:50 - 10:35", 
            "10:50 - 11:35", "11:40 - 12:25", "12:40 - 13:25",
            "13:35 - 14:20", "14:25 - 15:10", "15:20 - 16:05"
        )
        etTime.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lessonHours))

        // 3. Sale (zgodnie z rezerwacjami)
        val roomsList = mutableListOf<String>()
        for (i in 17..35) roomsList.add(i.toString())
        for (i in 105..126) roomsList.add(i.toString())
        for (i in 201..215) roomsList.add(i.toString())
        roomsList.addAll(listOf("SG-1", "SG-2", "SG-3", "Informatyczna 1"))
        etRoom.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roomsList))

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                etDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnAddPerm.setOnClickListener {
            val className = etClass.text.toString().trim()
            val day = etDay.text.toString().trim().lowercase()
            val time = etTime.text.toString().trim()
            val room = etRoom.text.toString().trim()
            val subj = etSubj.text.toString().trim()

            if (className.isEmpty() || day.isEmpty() || time.isEmpty() || subj.isEmpty()) {
                Toast.makeText(context, "Uzupełnij wymagane pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lesson = hashMapOf("time" to time, "subject" to subj, "day" to day, "room" to room)
            db.collection("schedules").document(className).collection("weekly").add(lesson)
                .addOnSuccessListener { 
                    Toast.makeText(context, "Dodano do planu stałego", Toast.LENGTH_SHORT).show()
                    etTime.setText(""); etSubj.setText("")
                }
        }

        btnAddChange.setOnClickListener {
            val className = etClass.text.toString().trim()
            val date = etDate.text.toString().trim()
            val subj = etChangeSubj.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (className.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(context, "Uzupełnij klasę, datę i godzinę", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isCancelled = subj.contains("odwoł", true) || subj.isEmpty()
            val change = hashMapOf(
                "date" to date,
                "newSubject" to subj,
                "time" to time,
                "isCancelled" to isCancelled
            )
            db.collection("schedules").document(className).collection("changes").add(change)
                .addOnSuccessListener { 
                    Toast.makeText(context, "Dodano zmianę w planie", Toast.LENGTH_SHORT).show()
                    etChangeSubj.setText(""); etDate.setText("")
                }
        }
    }
}
