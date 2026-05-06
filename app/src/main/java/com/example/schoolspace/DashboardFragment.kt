package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("grades")
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
        }
    }
}
