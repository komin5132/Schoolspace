package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherDashboardFragment : Fragment(R.layout.fragment_teacher_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtTutorClass = view.findViewById<TextView>(R.id.txtTutorClassName)
        val btnViewSchedule = view.findViewById<Button>(R.id.btnViewClassSchedule)
        val cardGrades = view.findViewById<View>(R.id.btnQuickGrades)
        val cardMessages = view.findViewById<View>(R.id.btnQuickMessages)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val tutorClass = doc.getString("class") ?: "Brak"
            txtTutorClass.text = if (tutorClass != "Brak") "Klasa $tutorClass" else "Brak przypisanej klasy"
            
            if (tutorClass == "Brak") {
                btnViewSchedule.visibility = View.GONE
            } else {
                btnViewSchedule.setOnClickListener {
                    (activity as? MainActivity)?.loadFragment(ScheduleFragment(), true)
                }
            }
        }

        cardGrades.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ManageGradesFragment(), true)
        }

        cardMessages.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(MessagesFragment(), true)
        }
    }
}
