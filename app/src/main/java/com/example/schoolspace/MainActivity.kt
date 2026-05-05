package com.example.schoolspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// Modele danych
data class Lesson(
    val day: String = "",
    val time: String = "",
    val subject: String = "",
    val room: String = "",
    val teacher: String = ""
)

data class ScheduleChange(
    val date: String = "",
    val time: String = "",
    val newSubject: String = "",
    val isCancelled: Boolean = false
)

data class Grade(
    val subject: String = "",
    val value: String = "",
    val description: String = "",
    val date: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userRole: String = "unassigned"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Zastosuj motyw przed super.onCreate
        val themePrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        // Domyślnie sprawdź czy system jest w trybie ciemnym jeśli nie ustawiono inaczej
        val isDarkMode = themePrefs.getBoolean("dark_mode", 
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        )
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0 

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Szybkie ładowanie roli z pamięci lokalnej, aby uniknąć białego ekranu
        loadRoleFromCache(bottomNav)
        checkUserRole(bottomNav)

        btnProfile.setOnClickListener { showProfileDialog() }

        bottomNav.setOnItemSelectedListener { item ->
            val nextFragment = when (item.itemId) {
                R.id.nav_dashboard, R.id.nav_admin_dashboard, R.id.nav_teacher_dashboard -> {
                    if (userRole == "admin") AdminDashboardFragment() else DashboardFragment()
                }
                R.id.nav_schedule -> ScheduleFragment()
                R.id.nav_manage_schedule -> ManageScheduleFragment()
                R.id.nav_manage_users -> ManageUsersFragment()
                R.id.nav_manage_grades -> ManageGradesFragment()
                R.id.nav_teacher_classes -> TeacherClassesFragment()
                R.id.nav_messages -> MessagesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            loadFragment(nextFragment, false)
            true
        }

        // Przywróć ostatni fragment po zmianie motywu
        if (savedInstanceState != null) {
            val lastTag = savedInstanceState.getString("last_fragment_tag")
            if (lastTag != null) {
                // Synchronizacja BottomNav z przywróconym fragmentem
                bottomNav.post {
                    when (lastTag) {
                        "AdminDashboardFragment", "DashboardFragment" -> bottomNav.selectedItemId = R.id.nav_dashboard
                        "ScheduleFragment" -> bottomNav.selectedItemId = R.id.nav_schedule
                        "ManageUsersFragment" -> bottomNav.selectedItemId = R.id.nav_manage_users
                        "ManageScheduleFragment" -> bottomNav.selectedItemId = R.id.nav_manage_schedule
                        "ManageGradesFragment" -> bottomNav.selectedItemId = R.id.nav_manage_grades
                        "TeacherClassesFragment" -> bottomNav.selectedItemId = R.id.nav_teacher_classes
                        "MessagesFragment" -> bottomNav.selectedItemId = R.id.nav_messages
                        "SettingsFragment" -> bottomNav.selectedItemId = R.id.nav_settings
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        currentFragment?.let {
            outState.putString("last_fragment_tag", it.javaClass.simpleName)
        }
    }

    private fun loadRoleFromCache(bottomNav: BottomNavigationView) {
        val currentEmail = auth.currentUser?.email ?: return
        val prefs = getSharedPreferences("saved_accounts", Context.MODE_PRIVATE)
        val accountsArray = JSONArray(prefs.getString("accounts_list", "[]"))
        
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            if (obj.getString("email") == currentEmail) {
                userRole = obj.getString("role")
                setupNavigation(bottomNav)
                break
            }
        }
    }

    private fun checkUserRole(bottomNav: BottomNavigationView) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val newRole = document.getString("role") ?: "unassigned"
                        val email = auth.currentUser?.email ?: ""
                        saveAccountLocally(email, newRole)
                        
                        // Aktualizuj nawigację tylko jeśli rola się zmieniła, 
                        // aby uniknąć zbędnego przeładowania UI i lagów
                        if (newRole != userRole) {
                            userRole = newRole
                            setupNavigation(bottomNav)
                        }
                    } else {
                        if (userRole != "unassigned") {
                            userRole = "unassigned"
                            setupNavigation(bottomNav)
                        }
                    }
                }
                .addOnFailureListener {
                    // W razie błędu sieci, zostajemy przy roli z cache (jeśli istnieje)
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
                bottomNav.inflateMenu(R.menu.admin_nav_menu)
                loadInitialFragment(AdminDashboardFragment())
            }
            "teacher" -> {
                bottomNav.visibility = View.VISIBLE
                bottomNav.inflateMenu(R.menu.teacher_nav_menu)
                loadInitialFragment(DashboardFragment())
            }
            "student" -> {
                bottomNav.visibility = View.VISIBLE
                bottomNav.inflateMenu(R.menu.bottom_nav_menu)
                loadInitialFragment(DashboardFragment())
            }
            else -> {
                bottomNav.visibility = View.GONE
                loadInitialFragment(UnassignedFragment())
            }
        }
    }

    private fun loadInitialFragment(fragment: Fragment) {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (current == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
        }
    }

    fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        // Krytyczne zabezpieczenie przed zapętleniem
        if (currentFragment != null && currentFragment::class == fragment::class) {
            return 
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .apply { if (addToBackStack) addToBackStack(null) }
            .commitAllowingStateLoss() // Bezpieczniejsze przy szybkich zmianach

        // Sync BottomNav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val itemId = when (fragment) {
            is AdminDashboardFragment, is DashboardFragment -> R.id.nav_dashboard
            is ScheduleFragment -> if (userRole == "admin") R.id.nav_manage_schedule else R.id.nav_schedule
            is ManageUsersFragment -> R.id.nav_manage_users
            is ManageGradesFragment -> R.id.nav_manage_grades
            is ManageScheduleFragment -> R.id.nav_manage_schedule
            is TeacherClassesFragment -> R.id.nav_teacher_classes
            is MessagesFragment -> R.id.nav_messages
            is SettingsFragment -> R.id.nav_settings
            else -> null
        }
        
        itemId?.let { id ->
            if (bottomNav.selectedItemId != id) {
                // Używamy post, aby uniknąć konfliktów w trakcie cyklu życia fragmentu
                bottomNav.post {
                    if (bottomNav.selectedItemId != id) {
                        bottomNav.selectedItemId = id
                    }
                }
            }
        }
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
            tvEmail.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

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
