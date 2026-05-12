package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val txtLastGrade = view.findViewById<TextView>(R.id.txtLastGrade)
        val btnSeeAll = view.findViewById<TextView>(R.id.btnSeeAllGrades)
        
        btnSeeAll.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(GradesFragment(), true)
        }
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            
            // 1. Ostatnia ocena
            db.collection("users").document(uid).collection("grades")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get().addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val g = docs.documents[0].toObject(Grade::class.java)
                        txtLastGrade.text = "Ostatnia ocena: ${g?.value} (${g?.subject})"
                    } else {
                        txtLastGrade.text = "Brak ocen"
                    }
                }

            // 2. Następna lekcja
            setupNextLessonWidget(view, uid)
        }
    }

    private fun setupNextLessonWidget(view: View, uid: String) {
        val db = FirebaseFirestore.getInstance()
        val layoutInfo = view.findViewById<View>(R.id.layoutNextLessonInfo)
        val txtNoLesson = view.findViewById<TextView>(R.id.txtNoNextLesson)
        val txtTime = view.findViewById<TextView>(R.id.txtNextLessonTime)
        val txtSubj = view.findViewById<TextView>(R.id.txtNextLessonSubject)
        val txtRoom = view.findViewById<TextView>(R.id.txtNextLessonRoom)

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userClass = userDoc.getString("class") ?: ""
            if (userClass.isEmpty() || userClass == "Brak") return@addOnSuccessListener

            val cal = Calendar.getInstance()
            val dayOfWeek = when(cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "poniedziałek"
                Calendar.TUESDAY -> "wtorek"
                Calendar.WEDNESDAY -> "środa"
                Calendar.THURSDAY -> "czwartek"
                Calendar.FRIDAY -> "piątek"
                else -> null
            }

            if (dayOfWeek == null) {
                txtNoLesson.text = "Dziś jest weekend - brak lekcji"
                return@addOnSuccessListener
            }

            val currentTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", 
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

            db.collection("schedules").document(userClass).collection("weekly")
                .whereEqualTo("day", dayOfWeek)
                .get().addOnSuccessListener { weeklyDocs ->
                    val lessons = weeklyDocs.documents
                        .map { it.toObject(Lesson::class.java)!! }
                        .filter { it.time > currentTime }
                        .sortedBy { it.time }

                    if (lessons.isNotEmpty()) {
                        val next = lessons[0]
                        txtTime.text = next.time
                        txtSubj.text = next.subject
                        txtRoom.text = if (next.room.isNotEmpty()) "Sala ${next.room}" else ""
                        
                        layoutInfo.visibility = View.VISIBLE
                        txtNoLesson.visibility = View.GONE
                    } else {
                        layoutInfo.visibility = View.GONE
                        txtNoLesson.visibility = View.VISIBLE
                        txtNoLesson.text = "To była ostatnia lekcja dzisiaj"
                    }
                }
        }
    }
}
