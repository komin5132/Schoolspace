package com.example.schoolspace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GradesFragment : Fragment(R.layout.fragment_grades) {

    data class SubjectGrades(
        val subjectName: String,
        val grades: List<Grade>,
        val average: Double
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvGrades)
        val txtAverage = view.findViewById<TextView>(R.id.txtOverallAverage)
        rv.layoutManager = LinearLayoutManager(context)

        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid).collection("grades")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().addOnSuccessListener { docs ->
                val allGrades = docs.toObjects(Grade::class.java).filterNotNull()
                val grouped = allGrades.groupBy { it.subject }
                val subjectGradesList = grouped.map { (name, grades) ->
                    SubjectGrades(name, grades, calculateWeightedAverage(grades))
                }.sortedBy { it.subjectName }
                rv.adapter = SubjectGradesAdapter(subjectGradesList)

                if (subjectGradesList.isNotEmpty()) {
                    val overallAvg = subjectGradesList.map { it.average }.average()
                    txtAverage.text = String.format(java.util.Locale.US, "Średnia: %.2f", overallAvg)
                } else {
                    txtAverage.text = "Średnia: 0.00"
                }
            }
    }

    private fun calculateWeightedAverage(grades: List<Grade>): Double {
        var sum = 0.0
        var weightSum = 0.0
        for (g in grades) {
            val valNum = parseGradeValue(g.value)
            if (valNum != null) {
                sum += valNum * g.weight
                weightSum += g.weight
            }
        }
        return if (weightSum > 0) sum / weightSum else 0.0
    }

    private fun parseGradeValue(s: String): Double? {
        return try {
            if (s.endsWith("-")) s.replace("-", "").toDouble() - 0.25
            else if (s.endsWith("+")) s.replace("+", "").toDouble() + 0.25
            else s.toDouble()
        } catch (e: Exception) { null }
    }

    inner class SubjectGradesAdapter(private val list: List<SubjectGrades>) : RecyclerView.Adapter<SubjectViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_subject_grades, parent, false)
            return SubjectViewHolder(v)
        }
        override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
            val sg = list[position]
            holder.name.text = sg.subjectName
            holder.grades.text = sg.grades.joinToString(", ") { it.value }
            holder.average.text = String.format(Locale.US, "%.2f", sg.average)
            holder.itemView.setOnClickListener { showSubjectDetails(sg) }
        }
        override fun getItemCount() = list.size
    }

    private fun showSubjectDetails(sg: SubjectGrades) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade_details, null)
        val rvDetails = dialogView.findViewById<RecyclerView>(R.id.rvGradeDetails)
        val txtSubj = dialogView.findViewById<TextView>(R.id.txtDetailSubject)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetails)

        txtSubj.text = sg.subjectName
        rvDetails.layoutManager = LinearLayoutManager(requireContext())
        rvDetails.adapter = DetailsAdapter(sg.grades)

        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class DetailsAdapter(private val list: List<Grade>) : RecyclerView.Adapter<DetailViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DetailViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_grade_detail_row, parent, false)
        )
        override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
            val g = list[position]
            holder.value.text = g.value
            holder.desc.text = if (g.description.isEmpty()) "Brak opisu" else g.description
            holder.weight.text = "waga: ${g.weight}"
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            holder.date.text = sdf.format(g.date.toDate())
        }
        override fun getItemCount() = list.size
    }

    class SubjectViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name = v.findViewById<TextView>(R.id.txtSubjectName)
        val grades = v.findViewById<TextView>(R.id.txtGradesList)
        val average = v.findViewById<TextView>(R.id.txtSubjectAverage)
    }

    class DetailViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val value = v.findViewById<TextView>(R.id.txtDetValue)
        val desc = v.findViewById<TextView>(R.id.txtDetDesc)
        val weight = v.findViewById<TextView>(R.id.txtDetWeight)
        val date = v.findViewById<TextView>(R.id.txtDetDate)
    }
}
