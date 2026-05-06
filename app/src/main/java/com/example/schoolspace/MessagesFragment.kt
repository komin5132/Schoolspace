package com.example.schoolspace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val rv = view.findViewById<RecyclerView>(R.id.rvMessages)
        val txtEmpty = view.findViewById<TextView>(R.id.txtEmptyMessages)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCompose)

        adapter = MessageAdapter(messageList) { message ->
            markAsRead(message)
            showDetails(message)
        }

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        fab.setOnClickListener { showComposeDialog() }

        loadMessages(txtEmpty)
    }

    private fun loadMessages(emptyView: TextView) {
        val userEmail = auth.currentUser?.email ?: return
        
        db.collection("messages")
            .whereEqualTo("receiverEmail", userEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                messageList.clear()
                snapshots?.forEach { doc ->
                    val msg = doc.toObject(Message::class.java).copy(id = doc.id)
                    messageList.add(msg)
                }
                
                adapter.notifyDataSetChanged()
                emptyView.visibility = if (messageList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun markAsRead(message: Message) {
        if (!message.isRead) {
            db.collection("messages").document(message.id).update("isRead", true)
        }
    }

    private fun showDetails(message: Message) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(message.subject)
            .setMessage("Od: ${message.senderEmail}\n\n${message.body}")
            .setPositiveButton("Zamknij", null)
            .setNeutralButton("Odpowiedz") { _, _ ->
                showComposeDialog(message.senderEmail, "RE: ${message.subject}")
            }
            .show()
    }

    private fun showComposeDialog(prefillEmail: String = "", prefillSubject: String = "") {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_compose_message, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.etReceiverEmail)
        val etSubject = dialogView.findViewById<EditText>(R.id.etMsgSubject)
        val etBody = dialogView.findViewById<EditText>(R.id.etMsgBody)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSendMessage)

        etEmail.setText(prefillEmail)
        etSubject.setText(prefillSubject)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnSend.setOnClickListener {
            val receiver = etEmail.text.toString().trim()
            val subject = etSubject.text.toString().trim()
            val body = etBody.text.toString().trim()

            if (receiver.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                Toast.makeText(context, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendMessage(receiver, subject, body)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendMessage(receiver: String, subject: String, body: String) {
        val user = auth.currentUser ?: return
        val message = Message(
            senderUid = user.uid,
            senderEmail = user.email ?: "",
            receiverEmail = receiver,
            subject = subject,
            body = body,
            timestamp = com.google.firebase.Timestamp.now(),
            isRead = false
        )

        // 1. Zapisz w systemie wewnętrznym aplikacji
        db.collection("messages").add(message)
            .addOnSuccessListener {
                Toast.makeText(context, "Wysłano wiadomość wewnętrzną", Toast.LENGTH_SHORT).show()
                
                // 2. Wyślij e-mail automatycznie przez rozszerzenie Firebase (Opcja B)
                sendAutomaticEmail(receiver, subject, body)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Błąd wysyłania", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendAutomaticEmail(receiver: String, subject: String, body: String) {
        val emailData = hashMapOf(
            "to" to receiver,
            "message" to hashMapOf(
                "subject" to subject,
                "text" to body,
                "html" to "<p>$body</p><br><br><i>Wysłano z aplikacji SchoolSpace</i>"
            )
        )

        // Dodanie do kolekcji 'mail' triggeruje rozszerzenie Firebase "Trigger Email"
        db.collection("mail").add(emailData)
            .addOnSuccessListener {
                Toast.makeText(context, "E-mail wysłany automatycznie", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Jeśli nie masz skonfigurowanego rozszerzenia, e-mail nie wyjdzie, 
                // ale wiadomość w aplikacji nadal będzie widoczna.
            }
    }

    inner class MessageAdapter(
        private val list: List<Message>,
        private val onClick: (Message) -> Unit
    ) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        inner class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val dot = v.findViewById<View>(R.id.viewUnreadDot)
            val sender = v.findViewById<TextView>(R.id.txtMsgSender)
            val subject = v.findViewById<TextView>(R.id.txtMsgSubject)
            val snippet = v.findViewById<TextView>(R.id.txtMsgBodySnippet)
            val date = v.findViewById<TextView>(R.id.txtMsgDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(v)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val m = list[position]
            holder.sender.text = m.senderEmail
            holder.subject.text = m.subject
            holder.snippet.text = m.body
            
            val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            holder.date.text = sdf.format(m.timestamp.toDate())

            holder.dot.visibility = if (m.isRead) View.GONE else View.VISIBLE
            
            holder.itemView.setOnClickListener { onClick(m) }
        }

        override fun getItemCount() = list.size
    }
}
