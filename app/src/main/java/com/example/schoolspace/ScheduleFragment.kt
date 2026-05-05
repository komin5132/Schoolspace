package com.example.schoolspace

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private var currentCalendar = Calendar.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateDateDisplay(view)
        loadSchedule(view)

        view.findViewById<ImageButton>(R.id.btnPrevDay)?.setOnClickListener {
            currentCalendar.add(Calendar.DAY_OF_YEAR, -1)
            updateDateDisplay(view)
            loadSchedule(view)
        }
        view.findViewById<ImageButton>(R.id.btnNextDay)?.setOnClickListener {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 1)
            updateDateDisplay(view)
            loadSchedule(view)
        }
    }

    private fun updateDateDisplay(view: View) {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("pl", "PL"))
        view.findViewById<TextView>(R.id.txtDate).text = sdf.format(currentCalendar.time)
    }

    private fun loadSchedule(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.listLessons)
        container.removeAllViews()

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userClass = userDoc.getString("class") ?: return@addOnSuccessListener

            val dayOfWeek = when(currentCalendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "poniedziałek"
                Calendar.TUESDAY -> "wtorek"
                Calendar.WEDNESDAY -> "środa"
                Calendar.THURSDAY -> "czwartek"
                Calendar.FRIDAY -> "piątek"
                else -> "weekend"
            }

            if (dayOfWeek == "weekend") {
                view.findViewById<TextView>(R.id.txtEmptySchedule).visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            // Pobierz plan stały
            db.collection("schedules").document(userClass).collection("weekly")
                .whereEqualTo("day", dayOfWeek)
                .get().addOnSuccessListener { weeklyDocs ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

                    // Pobierz zmiany na ten dzień
                    db.collection("schedules").document(userClass).collection("changes")
                        .whereEqualTo("date", dateStr)
                        .get().addOnSuccessListener { changeDocs ->

                            val changes = changeDocs.documents.map { it.data }

                            for (doc in weeklyDocs) {
                                val lessonTime = doc.getString("time") ?: ""
                                val lessonSubj = doc.getString("subject") ?: ""

                                val change = changes.find { it?.get("time") == lessonTime }

                                val itemView = layoutInflater.inflate(R.layout.item_lesson, container, false)
                                itemView.findViewById<TextView>(R.id.txtLessonTime).text = lessonTime

                                val txtSubj = itemView.findViewById<TextView>(R.id.txtLessonSubject)
                                val txtChange = itemView.findViewById<TextView>(R.id.txtLessonChange)

                                txtSubj.text = lessonSubj
                                if (change != null) {
                                    txtSubj.paintFlags = txtSubj.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                                    txtSubj.alpha = 0.5f
                                    txtChange.visibility = View.VISIBLE
                                    val newSubj = change["newSubject"] as? String
                                    val isCancelled = change["isCancelled"] as? Boolean ?: false
                                    txtChange.text = if (isCancelled) "LEKCJA ODWOŁANA" else "ZMIANA: $newSubj"
                                }

                                container.addView(itemView)
                            }

                            view.findViewById<TextView>(R.id.txtEmptySchedule).visibility =
                                if (container.childCount == 0) View.VISIBLE else View.GONE
                        }
                }
        }
    }
}
