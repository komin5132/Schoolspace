package com.example.schoolspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userRole: String = "unassigned"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0 

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        checkUserRole(bottomNav)

        btnProfile.setOnClickListener { showProfileDialog() }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> {
                    if (userRole == "admin") AdminDashboardFragment() else DashboardFragment()
                }
                R.id.nav_schedule -> ScheduleFragment()
                R.id.nav_teacher_classes -> TeacherClassesFragment()
                R.id.nav_messages -> MessagesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            loadFragment(fragment, false)
            true
        }
    }

    private fun checkUserRole(bottomNav: BottomNavigationView) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userRole = document.getString("role") ?: "unassigned"
                        val email = auth.currentUser?.email ?: ""
                        saveAccountLocally(email, userRole)
                        setupNavigation(bottomNav)
                    } else {
                        userRole = "unassigned"
                        setupNavigation(bottomNav)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Błąd pobierania roli", Toast.LENGTH_SHORT).show()
                    userRole = "unassigned"
                    setupNavigation(bottomNav)
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun saveAccountLocally(email: String, role: String) {
        if (email.isEmpty()) return
        val prefs = getSharedPreferences("saved_accounts", Context.MODE_PRIVATE)
        val accountsJson = prefs.getString("accounts_list", "[]")
        val accountsArray = JSONArray(accountsJson)
        
        var exists = false
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            if (obj.getString("email") == email) {
                obj.put("role", role)
                exists = true
                break
            }
        }
        
        if (!exists) {
            val newAcc = JSONObject()
            newAcc.put("email", email)
            newAcc.put("role", role)
            accountsArray.put(newAcc)
        }
        
        prefs.edit().putString("accounts_list", accountsArray.toString()).apply()
    }

    private fun setupNavigation(bottomNav: BottomNavigationView) {
        bottomNav.menu.clear()
        when (userRole) {
            "admin" -> {
                bottomNav.visibility = View.VISIBLE
                bottomNav.inflateMenu(R.menu.bottom_nav_menu)
                loadFragment(AdminDashboardFragment(), false)
            }
            "teacher" -> {
                bottomNav.visibility = View.VISIBLE
                bottomNav.inflateMenu(R.menu.teacher_nav_menu)
                loadFragment(DashboardFragment(), false)
            }
            "student" -> {
                bottomNav.visibility = View.VISIBLE
                bottomNav.inflateMenu(R.menu.bottom_nav_menu)
                loadFragment(DashboardFragment(), false)
            }
            else -> {
                bottomNav.visibility = View.GONE
                loadFragment(UnassignedFragment(), false)
            }
        }
    }

    fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun showProfileDialog() {
        val user = auth.currentUser
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null)
        
        val txtEmail = dialogView.findViewById<TextView>(R.id.txtUserEmail)
        val txtSubInfo = dialogView.findViewById<TextView>(R.id.txtUserSubInfo)
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogoutDialog)
        val btnAddAccount = dialogView.findViewById<Button>(R.id.btnAddAccount)
        val layoutSavedAccounts = dialogView.findViewById<LinearLayout>(R.id.layoutSavedAccounts)

        txtEmail.text = user?.email ?: "Użytkownik"

        val uid = user?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: "unassigned"
                    val className = document.getString("class") ?: ""
                    val roleDisplay = translateRole(role)
                    txtSubInfo.text = if (className.isNotEmpty() && className != "Brak") "Klasa $className | $roleDisplay" else roleDisplay
                }
            }
        }

        refreshSavedAccountsList(layoutSavedAccounts)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        btnLogout.setOnClickListener {
            auth.signOut()
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnAddAccount.setOnClickListener {
            auth.signOut() 
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        dialog.show()
    }

    private fun refreshSavedAccountsList(container: LinearLayout) {
        container.removeAllViews()
        val currentEmail = auth.currentUser?.email
        val prefs = getSharedPreferences("saved_accounts", Context.MODE_PRIVATE)
        val accountsArray = JSONArray(prefs.getString("accounts_list", "[]"))

        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            val email = obj.getString("email")
            val role = obj.getString("role")

            if (email == currentEmail) continue

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            row.setPadding(0, 8, 0, 8)
            row.gravity = android.view.Gravity.CENTER_VERTICAL

            val infoLayout = LinearLayout(this)
            infoLayout.orientation = LinearLayout.VERTICAL
            infoLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val tvEmail = TextView(this)
            tvEmail.text = email
            tvEmail.textSize = 14f
            tvEmail.setTextColor(ContextCompat.getColor(this, android.R.color.black))

            val tvRole = TextView(this)
            tvRole.text = translateRole(role)
            tvRole.textSize = 12f
            tvRole.alpha = 0.6f

            infoLayout.addView(tvEmail)
            infoLayout.addView(tvRole)

            val btnDelete = ImageButton(this)
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete)
            btnDelete.background = null
            btnDelete.setOnClickListener {
                removeAccountLocally(email)
                refreshSavedAccountsList(container)
            }

            row.addView(infoLayout)
            row.addView(btnDelete)
            
            row.setOnClickListener {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("PREFILL_EMAIL", email)
                intent.putExtra("AUTO_LOGIN", true)
                startActivity(intent)
                finish()
            }

            container.addView(row)
        }
    }

    private fun removeAccountLocally(email: String) {
        val prefs = getSharedPreferences("saved_accounts", Context.MODE_PRIVATE)
        val accountsArray = JSONArray(prefs.getString("accounts_list", "[]"))
        val newList = JSONArray()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            if (obj.getString("email") != email) {
                newList.put(obj)
            }
        }
        prefs.edit().putString("accounts_list", newList.toString()).apply()

        try {
            val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val securePrefs = EncryptedSharedPreferences.create(
                this, "secure_user_creds", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            securePrefs.edit().remove(email).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun translateRole(role: String): String {
        return when(role) {
            "student" -> "Uczeń"
            "teacher" -> "Nauczyciel"
            "admin" -> "Administrator"
            else -> "Brak przydziału"
        }
    }
}

// Fragmenty
class DashboardFragment : Fragment(R.layout.fragment_dashboard)
class ScheduleFragment : Fragment(R.layout.fragment_schedule)
class TeacherClassesFragment : Fragment(R.layout.fragment_dashboard) 
class MessagesFragment : Fragment(R.layout.fragment_messages)
class SettingsFragment : Fragment(R.layout.fragment_settings)

class UnassignedFragment : Fragment(R.layout.fragment_unassigned) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnRefreshRole)?.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.cardManageUsers).setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ManageUsersFragment(), true)
        }
        view.findViewById<View>(R.id.cardManageSchedule).setOnClickListener {
            Toast.makeText(context, "Zarządzanie planem lekcji (Wkrótce)", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.cardManageGrades).setOnClickListener {
            Toast.makeText(context, "Zarządzanie ocenami (Wkrótce)", Toast.LENGTH_SHORT).show()
        }
    }
}
