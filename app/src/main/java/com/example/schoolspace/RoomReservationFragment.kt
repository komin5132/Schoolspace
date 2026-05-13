package com.example.schoolspace

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RoomReservationFragment : Fragment(R.layout.fragment_room_reservation) {

    private val roomsList = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilRoom = view.findViewById<TextInputLayout>(R.id.tilRoomNumber)
        val etRoom = view.findViewById<AutoCompleteTextView>(R.id.etRoomNumber)
        val etTopic = view.findViewById<EditText>(R.id.etReservationTopic)
        val etDate = view.findViewById<EditText>(R.id.etReservationDate)
        val etTimeStart = view.findViewById<EditText>(R.id.etReservationTimeStart)
        val etTimeEnd = view.findViewById<EditText>(R.id.etReservationTimeEnd)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitReservation)
        val rv = view.findViewById<RecyclerView>(R.id.rvMyReservations)

        rv.layoutManager = LinearLayoutManager(context)
        loadReservations(rv)

        // Budowanie listy sal
        roomsList.clear()
        for (i in 17..35) roomsList.add(i.toString())
        for (i in 105..126) roomsList.add(i.toString())
        for (i in 201..215) roomsList.add(i.toString())
        roomsList.addAll(listOf("SG-1", "SG-2", "SG-3", "Informatyczna 1"))
        
        val roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roomsList)
        etRoom.setAdapter(roomAdapter)

        // Ścisła walidacja sali w czasie rzeczywistym
        etRoom.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString().trim()
                if (input.isNotEmpty() && !roomsList.contains(input)) {
                    tilRoom.error = "Wybrana sala nie istnieje w systemie"
                } else {
                    tilRoom.error = null
                }
            }
        })

        // Automatyczne pokazywanie dropdowna przy kliknięciu dla lepszego UX
        etRoom.setOnClickListener {
            (it as? AutoCompleteTextView)?.showDropDown()
        }

        val cal = Calendar.getInstance()

        etDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        etTimeStart.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, min ->
                etTimeStart.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min))
                // Automatyczne ustawienie końca na +1h dla wygody
                val endH = if (h == 23) 23 else h + 1
                val endMin = if (h == 23) 59 else min
                etTimeEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", endH, endMin))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        etTimeEnd.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, min ->
                etTimeEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnSubmit.setOnClickListener {
            val room = etRoom.text.toString().trim()
            val topic = etTopic.text.toString().trim()
            val date = etDate.text.toString()
            val tStart = etTimeStart.text.toString()
            val tEnd = etTimeEnd.text.toString()

            if (room.isEmpty() || date.isEmpty() || tStart.isEmpty() || tEnd.isEmpty() || topic.isEmpty()) {
                Toast.makeText(context, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!roomsList.contains(room)) {
                tilRoom.error = "Wybierz salę z listy!"
                return@setOnClickListener
            } else {
                tilRoom.error = null
            }

            // Walidacja czasu (max 1h)
            if (!validateDuration(tStart, tEnd)) {
                Toast.makeText(context, "Maksymalny czas rezerwacji to 1 godzina", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            
            // Sprawdzenie konfliktów
            db.collection("room_reservations")
                .whereEqualTo("room", room)
                .whereEqualTo("date", date)
                .get().addOnSuccessListener { docs ->
                    val hasConflict = docs.documents.any { doc ->
                        val existingStart = doc.getString("timeStart") ?: ""
                        val existingEnd = doc.getString("timeEnd") ?: ""
                        checkOverlap(tStart, tEnd, existingStart, existingEnd)
                    }

                    if (hasConflict) {
                        Toast.makeText(context, "Sala jest już zarezerwowana w tym czasie", Toast.LENGTH_LONG).show()
                    } else {
                        performReservation(room, topic, date, tStart, tEnd, rv)
                    }
                }
        }
    }

    private fun validateDuration(start: String, end: String): Boolean {
        try {
            val s = start.split(":")
            val e = end.split(":")
            val startMin = s[0].toInt() * 60 + s[1].toInt()
            val endMin = e[0].toInt() * 60 + e[1].toInt()
            val diff = endMin - startMin
            
            // Logika: czas trwania musi być większy od 0 i nie większy niż 60 minut
            return diff in 1..60
        } catch (e: Exception) { return false }
    }

    private fun checkOverlap(s1: String, e1: String, s2: String, e2: String): Boolean {
        fun toMin(t: String): Int {
            val p = t.split(":")
            return p[0].toInt() * 60 + p[1].toInt()
        }
        val start1 = toMin(s1); val end1 = toMin(e1)
        val start2 = toMin(s2); val end2 = toMin(e2)
        return start1 < end2 && start2 < end1
    }

    private fun performReservation(room: String, topic: String, date: String, tStart: String, tEnd: String, rv: RecyclerView) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        val reservation = hashMapOf(
            "uid" to uid,
            "room" to room,
            "topic" to topic,
            "date" to date,
            "timeStart" to tStart,
            "timeEnd" to tEnd,
            "time" to "$tStart - $tEnd",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("room_reservations").add(reservation).addOnSuccessListener {
            Toast.makeText(context, "Zarezerwowano salę $room", Toast.LENGTH_SHORT).show()
            view?.findViewById<AutoCompleteTextView>(R.id.etRoomNumber)?.setText("")
            view?.findViewById<EditText>(R.id.etReservationTopic)?.setText("")
            view?.findViewById<EditText>(R.id.etReservationDate)?.setText("")
            view?.findViewById<EditText>(R.id.etReservationTimeStart)?.setText("")
            view?.findViewById<EditText>(R.id.etReservationTimeEnd)?.setText("")
            loadReservations(rv)
        }
    }

    private fun loadReservations(rv: RecyclerView) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("room_reservations")
            .whereEqualTo("uid", uid)
            .get().addOnSuccessListener { docs ->
                val list = docs.documents.map { doc ->
                    val time = doc.getString("time") ?: "${doc.getString("timeStart")} - ${doc.getString("timeEnd")}"
                    Reservation(
                        doc.getString("room") ?: "",
                        doc.getString("topic") ?: "",
                        doc.getString("date") ?: "",
                        time,
                        doc.getTimestamp("timestamp")?.toDate() ?: Date(),
                        doc.getString("uid") ?: ""
                    )
                }.sortedByDescending { it.timestamp }
                rv.adapter = ReservationAdapter(list)
            }
    }

    class ReservationAdapter(private val list: List<Reservation>) : RecyclerView.Adapter<ReservationAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val room: TextView = v.findViewById(R.id.txtResRoom)
            val topic: TextView = v.findViewById(R.id.txtResTopic)
            val dateTime: TextView = v.findViewById(R.id.txtResDateTime)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reservation, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val res = list[position]
            holder.room.text = "Sala ${res.room}"
            holder.topic.text = res.topic
            holder.dateTime.text = "${res.date} | ${res.time}"
        }
        override fun getItemCount() = list.size
    }
}
