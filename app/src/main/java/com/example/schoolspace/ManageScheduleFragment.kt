package com.example.schoolspace

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ManageScheduleFragment : Fragment(R.layout.fragment_manage_schedule) {
    
    private var selectedLessonsList = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etClass = view.findViewById<EditText>(R.id.etTargetClass)
        val etDay = view.findViewById<AutoCompleteTextView>(R.id.etLessonDay)
        val etTime = view.findViewById<AutoCompleteTextView>(R.id.etLessonTime)
        val etRoom = view.findViewById<AutoCompleteTextView>(R.id.etLessonRoom)
        val etSubj = view.findViewById<EditText>(R.id.etLessonSubject)
        val btnAddPerm = view.findViewById<Button>(R.id.btnAddLessonPermanent)

        val etDate = view.findViewById<EditText>(R.id.etChangeDate)
        val etChangeTime = view.findViewById<AutoCompleteTextView>(R.id.etChangeTime)
        val etChangeSubj = view.findViewById<EditText>(R.id.etChangeSubject)
        val btnAddChange = view.findViewById<Button>(R.id.btnAddLessonChange)
        val toggleGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleChangeType)
        val tilChangeSubj = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilChangeSubject)

        // Sekcje do ukrywania/pokazywania
        val mainToggle = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleMainMode)
        val sectionPermanentLabel = view.findViewById<View>(R.id.txtLabelPermanent)
        val sectionPermanentForm = view.findViewById<View>(R.id.cardPermanentForm)
        val sectionChangeLabel = view.findViewById<View>(R.id.txtLabelChange)
        val sectionChangeForm = view.findViewById<View>(R.id.cardChangeForm)

        val db = FirebaseFirestore.getInstance()

        // Domyślny stan (Plan Stały)
        sectionChangeLabel.visibility = View.GONE
        sectionChangeForm.visibility = View.GONE

        mainToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnModePermanent) {
                    sectionPermanentLabel.visibility = View.VISIBLE
                    sectionPermanentForm.visibility = View.VISIBLE
                    sectionChangeLabel.visibility = View.GONE
                    sectionChangeForm.visibility = View.GONE
                } else {
                    sectionPermanentLabel.visibility = View.GONE
                    sectionPermanentForm.visibility = View.GONE
                    sectionChangeLabel.visibility = View.VISIBLE
                    sectionChangeForm.visibility = View.VISIBLE
                }
            }
        }

        // Obsługa przełącznika typu zmiany (Zastępstwo / Odwołana)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnTypeCancel) {
                    tilChangeSubj.hint = "Powód odwołania (opcjonalnie)"
                    etChangeSubj.setText("ODWOŁANA")
                } else {
                    tilChangeSubj.hint = "Nowy przedmiot / Zastępstwo"
                    if (etChangeSubj.text.toString() == "ODWOŁANA") etChangeSubj.setText("")
                }
            }
        }

        // 1. Dni tygodnia
        val days = arrayOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek")
        etDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        // 2. Godziny lekcyjne (Dla Planu Stałego)
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

        // INTELIGENTNY DROPDOWN (Wariant 1)
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                etDate.setText(dateStr)
                
                val className = etClass.text.toString().trim()
                if (className.isNotEmpty()) {
                    val cal2 = Calendar.getInstance()
                    cal2.set(y, m, d)
                    val dayName = when(cal2.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "poniedziałek"
                        Calendar.TUESDAY -> "wtorek"
                        Calendar.WEDNESDAY -> "środa"
                        Calendar.THURSDAY -> "czwartek"
                        Calendar.FRIDAY -> "piątek"
                        else -> "weekend"
                    }
                    loadLessonsForChange(className, dayName, etChangeTime, dateStr)
                } else {
                    Toast.makeText(context, "Najpierw wpisz klasę!", Toast.LENGTH_SHORT).show()
                }
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
            val rawTimeSelection = etChangeTime.text.toString()
            val subj = etChangeSubj.text.toString().trim()
            val isCancelled = toggleGroup.checkedButtonId == R.id.btnTypeCancel

            if (className.isEmpty() || date.isEmpty() || rawTimeSelection.isEmpty()) {
                Toast.makeText(context, "Uzupełnij klasę, datę i wybierz lekcję", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Wyciągamy samą godzinę z zaznaczonej opcji dropdowna (np. "08:00 - 08:45")
            val time = rawTimeSelection.split("|")[0].trim()

            val change = hashMapOf(
                "date" to date,
                "newSubject" to if (isCancelled) (if (subj.isEmpty() || subj == "ODWOŁANA") "LEKCJA ODWOŁANA" else subj) else subj,
                "time" to time,
                "isCancelled" to isCancelled
            )
            db.collection("schedules").document(className).collection("changes").add(change)
                .addOnSuccessListener { 
                    Toast.makeText(context, "Zapisano zmianę w planie", Toast.LENGTH_SHORT).show()
                    etChangeSubj.setText(""); etDate.setText(""); etChangeTime.setText("")
                }
        }
    }

    private fun loadLessonsForChange(className: String, dayName: String, dropdown: AutoCompleteTextView, dateStr: String) {
        if (dayName == "weekend") {
            dropdown.setText("Weekend - brak lekcji")
            dropdown.setAdapter(null)
            return
        }

        val db = FirebaseFirestore.getInstance()
        
        // Najpierw pobierz plan stały
        db.collection("schedules").document(className).collection("weekly")
            .whereEqualTo("day", dayName)
            .get().addOnSuccessListener { weeklyDocs ->
                
                // Potem pobierz istniejące zmiany na ten konkretny dzień
                db.collection("schedules").document(className).collection("changes")
                    .whereEqualTo("date", dateStr)
                    .get().addOnSuccessListener { changeDocs ->
                        
                        val changesMap = changeDocs.documents.associate { 
                            (it.getString("time") ?: "") to it
                        }

                        val lessons = weeklyDocs.documents.map { 
                            val time = it.getString("time") ?: ""
                            val permanentSubject = it.getString("subject") ?: ""
                            
                            // Sprawdź czy jest już zmiana dla tej godziny
                            val change = changesMap[time]
                            if (change != null) {
                                val isCancelled = change.getBoolean("isCancelled") ?: false
                                val newSubj = change.getString("newSubject") ?: "ZMIANA"
                                if (isCancelled) {
                                    "$time | [ODWOŁANA] $permanentSubject"
                                } else {
                                    "$time | [ZASTĘPSTWO: $newSubj]"
                                }
                            } else {
                                "$time | $permanentSubject"
                            }
                        }.sorted()

                        if (lessons.isEmpty()) {
                            dropdown.setText("Brak zaplanowanych lekcji")
                            dropdown.setAdapter(null)
                        } else {
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lessons)
                            dropdown.setAdapter(adapter)
                            dropdown.setText("") 
                            dropdown.showDropDown()
                        }
                    }
            }
    }
}
