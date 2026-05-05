package com.example.schoolspace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore

class ManageUsersFragment : Fragment(R.layout.fragment_manage_users) {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val filteredList = mutableListOf<User>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        
        val rvUsers = view.findViewById<RecyclerView>(R.id.rvUsers)
        val etSearch = view.findViewById<EditText>(R.id.etSearchUser)
        
        adapter = UserAdapter(filteredList) { user -> showEditUserDialog(user) }
        rvUsers.adapter = adapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadUsers()
    }

    private fun filterUsers(query: String) {
        val trimmedQuery = query.trim()
        filteredList.clear()
        if (trimmedQuery.isEmpty()) {
            filteredList.addAll(userList)
        } else {
            userList.forEach { user ->
                if (user.email.contains(trimmedQuery, ignoreCase = true)) {
                    filteredList.add(user)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadUsers() {
        val etSearch = view?.findViewById<EditText>(R.id.etSearchUser)
        db.collection("users").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            userList.clear()
            value?.forEach { doc ->
                val user = User(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "unassigned",
                    className = doc.getString("class") ?: "",
                    isTutor = doc.getBoolean("isTutor") ?: false
                )
                userList.add(user)
            }
            filterUsers(etSearch?.text?.toString() ?: "")
        }
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val tilClass = dialogView.findViewById<View>(R.id.tilUserClass)
        val etClass = dialogView.findViewById<EditText>(R.id.etUserClass)
        val cbIsTutor = dialogView.findViewById<CheckBox>(R.id.cbIsTutor)

        // Setup Spinner
        val roles = arrayOf("unassigned", "student", "teacher", "admin")
        val adapterRole = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapterRole.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapterRole
        
        // Ustawienie aktualnych wartości
        spinnerRole.setSelection(roles.indexOf(user.role))
        etClass.setText(user.className)
        cbIsTutor.isChecked = user.isTutor

        // Widoczność pól zależna od roli
        tilClass.visibility = if (user.role == "student" || user.role == "teacher") View.VISIBLE else View.GONE
        cbIsTutor.visibility = if (user.role == "teacher") View.VISIBLE else View.GONE
        
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRole = roles[position]
                tilClass.visibility = if (selectedRole == "student" || selectedRole == "teacher") View.VISIBLE else View.GONE
                cbIsTutor.visibility = if (selectedRole == "teacher") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edytuj użytkownika")
            .setMessage(user.email)
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val newRole = spinnerRole.selectedItem.toString()
                val newClass = if (newRole == "student" || newRole == "teacher") etClass.text.toString().trim() else ""
                val isTutor = if (newRole == "teacher") cbIsTutor.isChecked else false
                
                val updates = hashMapOf<String, Any>(
                    "role" to newRole,
                    "class" to newClass,
                    "isTutor" to isTutor
                )
                
                db.collection("users").document(user.uid).update(updates)
                    .addOnSuccessListener { Toast.makeText(context, "Zaktualizowano dane", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    data class User(val uid: String, val email: String, val role: String, val className: String, val isTutor: Boolean)

    class UserAdapter(private val users: List<User>, private val onEdit: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val email: TextView = view.findViewById(R.id.tvUserEmail)
            val details: TextView = view.findViewById(R.id.tvUserDetails)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEditUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.email.text = user.email
            val tutorInfo = if (user.isTutor) " (Wychowawca)" else ""
            val classInfo = if (user.role == "admin" || user.role == "unassigned") "" else " | Klasa: ${if(user.className.isEmpty()) "Brak" else user.className}"
            holder.details.text = "${user.role}$tutorInfo$classInfo"
            holder.btnEdit.setOnClickListener { onEdit(user) }
        }

        override fun getItemCount() = users.size
    }
}
