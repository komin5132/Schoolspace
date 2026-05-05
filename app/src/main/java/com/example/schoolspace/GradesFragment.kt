package com.example.schoolspace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GradesFragment : Fragment(R.layout.fragment_grades) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<RecyclerView>(R.id.rvGrades) ?: return
        container.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid).collection("grades")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                try {
                    val grades = docs.toObjects(Grade::class.java).filterNotNull()
                    container.adapter = GradesAdapter(grades)
                } catch (e: Exception) {
                    android.util.Log.e("GradesFragment", "Error parsing grades", e)
                    container.adapter = GradesAdapter(emptyList())
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("GradesFragment", "Error fetching grades", e)
                container.adapter = GradesAdapter(emptyList())
            }
    }

    private inner class GradesAdapter(private val grades: List<Grade>) : RecyclerView.Adapter<GradeViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_grade, parent, false)
            return GradeViewHolder(v)
        }

        override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
            val g = grades.getOrNull(position) ?: return

            holder.value.text = g.value.takeIf { it.isNotEmpty() } ?: "N/A"
            holder.subject.text = g.subject.takeIf { it.isNotEmpty() } ?: "Nieznany przedmiot"
            holder.desc.text = g.description.takeIf { it.isNotEmpty() } ?: ""

            try {
                val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
                holder.date.text = sdf.format(g.date.toDate())
            } catch (e: Exception) {
                holder.date.text = "N/A"
                android.util.Log.e("GradeViewHolder", "Error formatting date", e)
            }
        }

        override fun getItemCount() = grades.size
    }

    class GradeViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val value: TextView = v.findViewById(R.id.txtGradeValue)
        val subject: TextView = v.findViewById(R.id.txtGradeSubject)
        val desc: TextView = v.findViewById(R.id.txtGradeDesc)
        val date: TextView = v.findViewById(R.id.txtGradeDate)
    }
}
