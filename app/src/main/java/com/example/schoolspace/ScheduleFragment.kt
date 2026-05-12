package com.example.schoolspace

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private var selectedDate = Date()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupDatePicker(view)
        loadDailySchedule(view)
        setupFab(view)
    }

    private fun setupFab(view: View) {
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAction)
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val role = doc.getString("role") ?: "student"
            Log.d("ScheduleFragment", "User role loaded for FAB: $role")
            
            fab.setOnClickListener {
                val activity = activity as? MainActivity
                if (activity != null) {
                    if (role == "student") {
                        Log.d("ScheduleFragment", "Navigating to RoomReservationFragment")
                        activity.loadFragment(RoomReservationFragment(), true)
                    } else {
                        Log.d("ScheduleFragment", "Navigating to ManageScheduleFragment")
                        activity.loadFragment(ManageScheduleFragment(), true)
                    }
                }
            }
        }
    }

    private fun setupDatePicker(view: View) {
        val rvDatePicker = view.findViewById<RecyclerView>(R.id.rvDatePicker)
        val txtMonth = view.findViewById<TextView>(R.id.txtCurrentMonth)
        
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        
        // Zacznij od poniedziałku zeszłego tygodnia
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_YEAR, -14)
        
        for (i in 0..730) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        rvDatePicker.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        // SnapHelper sprawia, że dni "zatrzaskują się" przy przesuwaniu
        val snapHelper = androidx.recyclerview.widget.LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvDatePicker)

        val adapter = DateAdapter(dates) { date ->
            selectedDate = date
            updateMonthDisplay(txtMonth)
            loadDailySchedule(view)
        }
        rvDatePicker.adapter = adapter
        
        // Przewiń do dzisiaj
        val today = Calendar.getInstance()
        val startIndex = dates.indexOfFirst { 
            val c = Calendar.getInstance().apply { time = it }
            c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            c.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }
        if (startIndex != -1) {
            rvDatePicker.scrollToPosition(startIndex)
        }

        updateMonthDisplay(txtMonth)
    }

    private fun updateMonthDisplay(textView: TextView) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("pl", "PL"))
        textView.text = sdf.format(selectedDate).uppercase()
    }

    private fun loadDailySchedule(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.listLessons)
        container.removeAllViews()
        val emptyText = view.findViewById<TextView>(R.id.txtEmptySchedule)

        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userClass = userDoc.getString("class") ?: ""
            if (userClass.isEmpty() || userClass == "Brak") {
                emptyText.text = "Nie jesteś przypisany do żadnej klasy"
                emptyText.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            val cal = Calendar.getInstance().apply { time = selectedDate }
            val dayOfWeek = when(cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "poniedziałek"
                Calendar.TUESDAY -> "wtorek"
                Calendar.WEDNESDAY -> "środa"
                Calendar.THURSDAY -> "czwartek"
                Calendar.FRIDAY -> "piątek"
                else -> "weekend"
            }

            if (dayOfWeek == "weekend") {
                emptyText.text = "Weekend - brak zajęć"
                emptyText.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            db.collection("schedules").document(userClass).collection("weekly")
                .whereEqualTo("day", dayOfWeek)
                .get().addOnSuccessListener { weeklyDocs ->
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)

                    db.collection("schedules").document(userClass).collection("changes")
                        .whereEqualTo("date", dateStr)
                        .get().addOnSuccessListener { changeDocs ->

                            val changes = changeDocs.documents.map { it.data }
                            val sortedDocs = weeklyDocs.documents.sortedBy { it.getString("time") }

                            for (doc in sortedDocs) {
                                val lessonTime = doc.getString("time") ?: ""
                                val lessonSubj = doc.getString("subject") ?: ""
                                val room = doc.getString("room") ?: ""

                                val change = changes.find { it?.get("time") == lessonTime }

                                val itemView = layoutInflater.inflate(R.layout.item_lesson, container, false)
                                itemView.findViewById<TextView>(R.id.txtLessonTime).text = lessonTime
                                itemView.findViewById<TextView>(R.id.txtLessonRoom).text = "Sala $room"

                                val txtSubj = itemView.findViewById<TextView>(R.id.txtLessonSubject)
                                val txtChange = itemView.findViewById<TextView>(R.id.txtLessonChange)

                                txtSubj.text = lessonSubj
                                if (change != null) {
                                    txtSubj.paintFlags = txtSubj.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                                    txtSubj.alpha = 0.5f
                                    txtChange.visibility = View.VISIBLE
                                    val newSubj = change["newSubject"] as? String
                                    val isCancelled = change["isCancelled"] as? Boolean ?: false
                                    txtChange.text = if (isCancelled) "ODWOŁANA" else "ZMIANA: $newSubj"
                                }

                                container.addView(itemView)
                            }

                            // Dodać rezerwacje sal do planu dnia
                            db.collection("room_reservations")
                                .whereEqualTo("uid", uid)
                                .whereEqualTo("date", dateStr)
                                .get().addOnSuccessListener { resDocs ->
                                    if (!resDocs.isEmpty) {
                                        val header = TextView(context).apply {
                                            text = "MOJE REZERWACJE"
                                            setPadding(0, 32, 0, 8)
                                            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.primary))
                                            setTypeface(null, android.graphics.Typeface.BOLD)
                                            textSize = 14f
                                        }
                                        container.addView(header)

                                        for (res in resDocs) {
                                            val resView = layoutInflater.inflate(R.layout.item_lesson, container, false)
                                            resView.findViewById<TextView>(R.id.txtLessonTime).text = res.getString("time")
                                            resView.findViewById<TextView>(R.id.txtLessonSubject).text = res.getString("topic") ?: "Rezerwacja Sali"
                                            resView.findViewById<TextView>(R.id.txtLessonRoom).text = "Sala ${res.getString("room")}"
                                            resView.alpha = 0.9f
                                            container.addView(resView)
                                        }
                                    }
                                    emptyText.visibility = if (container.childCount == 0) View.VISIBLE else View.GONE
                                    if (container.childCount == 0) emptyText.text = "Brak planu na ten dzień"
                                }
                        }
                }
        }
    }
}
