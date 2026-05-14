package com.example.schoolspace

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
import com.google.android.material.button.MaterialButton
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
    private var isTrashView = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val rv = view.findViewById<RecyclerView>(R.id.rvMessages)
        val txtEmpty = view.findViewById<TextView>(R.id.txtEmptyMessages)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabCompose)
        val btnTrash = view.findViewById<MaterialButton>(R.id.btnTrash)
        val txtTitle = view.findViewById<TextView>(R.id.txtMessagesTitle)

        adapter = MessageAdapter(messageList) { message ->
            if (isTrashView) {
                showTrashOptions(message)
            } else {
                markAsRead(message)
                showDetails(message)
            }
        }

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        fab.setOnClickListener { showComposeDialog() }

        btnTrash.setOnClickListener {
            isTrashView = !isTrashView
            if (isTrashView) {
                txtTitle.text = "Kosz"
                btnTrash.text = "Wróć"
                btnTrash.setIconResource(R.drawable.ic_back)
                fab.visibility = View.GONE
                checkAndDeleteOldTrash()
            } else {
                txtTitle.text = "Wiadomości"
                btnTrash.text = "Kosz"
                btnTrash.setIconResource(R.drawable.ic_trash)
                fab.visibility = View.VISIBLE
            }
            loadMessages(txtEmpty)
        }

        loadMessages(txtEmpty)
    }

    private fun loadMessages(emptyView: TextView) {
        val userEmail = auth.currentUser?.email ?: return
        
        db.collection("messages")
            .whereEqualTo("receiverEmail", userEmail)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("MessagesFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }
                
                val allFetchedMessages = snapshots?.map { doc ->
                    doc.toObject(Message::class.java).copy(id = doc.id)
                } ?: emptyList()

                // Filtrowanie i sortowanie po stronie aplikacji, aby uniknąć błędów indeksowania Firestore
                messageList.clear()
                val filtered = allFetchedMessages.filter { it.isDeleted == isTrashView }
                messageList.addAll(filtered.sortedByDescending { it.timestamp })
                
                adapter.notifyDataSetChanged()
                emptyView.text = if (isTrashView) "Kosz jest pusty" else "Brak wiadomości"
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
            .setNegativeButton("Usuń") { _, _ -> moveToTrash(message) }
            .setNeutralButton("Odpowiedz") { _, _ ->
                showComposeDialog(message.senderEmail, "RE: ${message.subject}")
            }
            .show()
    }

    private fun moveToTrash(message: Message) {
        db.collection("messages").document(message.id).update(
            "isDeleted", true,
            "deletedAt", com.google.firebase.Timestamp.now()
        ).addOnSuccessListener {
            Toast.makeText(context, "Przeniesiono do kosza", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTrashOptions(message: Message) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(message.deletedAt?.toDate() ?: Date())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trash_details, null)
        
        val txtSubject = dialogView.findViewById<TextView>(R.id.txtTrashSubject)
        val txtContent = dialogView.findViewById<TextView>(R.id.txtTrashContent)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnTrashDeleteForever)
        val btnRestore = dialogView.findViewById<Button>(R.id.btnTrashRestore)
        val btnClose = dialogView.findViewById<Button>(R.id.btnTrashClose)

        txtSubject.text = message.subject
        txtContent.text = "Od: ${message.senderEmail}\n\n${message.body}\n\n---\nPrzeniesiono do kosza: $dateStr"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        btnDelete.setOnClickListener {
            deletePermanently(message)
            dialog.dismiss()
        }
        btnRestore.setOnClickListener {
            restoreMessage(message)
            dialog.dismiss()
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun restoreMessage(message: Message) {
        db.collection("messages").document(message.id).update(
            "isDeleted", false,
            "deletedAt", null
        ).addOnSuccessListener {
            Toast.makeText(context, "Wiadomość przywrócona", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePermanently(message: Message) {
        db.collection("messages").document(message.id).delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Wiadomość usunięta trwale", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAndDeleteOldTrash() {
        val userEmail = auth.currentUser?.email ?: return
        val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
        
        db.collection("messages")
            .whereEqualTo("receiverEmail", userEmail)
            .whereEqualTo("isDeleted", true)
            .get().addOnSuccessListener { docs ->
                docs.forEach { doc ->
                    val deletedAt = doc.getTimestamp("deletedAt")?.toDate()
                    if (deletedAt != null && deletedAt.before(thirtyDaysAgo)) {
                        doc.reference.delete()
                    }
                }
            }
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
            isRead = false,
            isDeleted = false
        )

        db.collection("messages").add(message)
            .addOnSuccessListener {
                Toast.makeText(context, "Wysłano wiadomość", Toast.LENGTH_SHORT).show()
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

            holder.dot.visibility = if (m.isRead || isTrashView) View.GONE else View.VISIBLE
            
            holder.itemView.setOnClickListener { onClick(m) }
        }

        override fun getItemCount() = list.size
    }
}
