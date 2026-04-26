package com.example.schoolspace

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userRole: String = "student"

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

        // Sprawdzamy rolę w Firestore
        checkUserRole(bottomNav)

        btnProfile.setOnClickListener { showProfileDialog() }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard, R.id.nav_teacher_dashboard -> DashboardFragment()
                R.id.nav_schedule -> ScheduleFragment()
                R.id.nav_teacher_classes -> TeacherClassesFragment()
                R.id.nav_messages -> MessagesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun checkUserRole(bottomNav: BottomNavigationView) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userRole = document.getString("role") ?: "student"
                        setupNavigation(bottomNav)
                    }
                }
        }
    }

    private fun setupNavigation(bottomNav: BottomNavigationView) {
        if (userRole == "teacher") {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.teacher_nav_menu)
            loadFragment(DashboardFragment()) // Start dla nauczyciela
        } else {
            // Uczeń ma już domyślne menu z XML, ale dla pewności:
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_menu)
            loadFragment(DashboardFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showProfileDialog() {
        val user = auth.currentUser
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null)
        
        val txtEmail = dialogView.findViewById<TextView>(R.id.txtUserEmail)
        val txtRole = dialogView.findViewById<TextView>(android.R.id.text1) // Placeholder dla roli
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogoutDialog)

        txtEmail.text = user?.email ?: "Użytkownik"
        // txtRole.text = "Rola: ${userRole.uppercase()}"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnLogout.setOnClickListener {
            auth.signOut()
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        dialog.show()
    }
}

// Fragmenty
class DashboardFragment : Fragment(R.layout.fragment_dashboard)
class ScheduleFragment : Fragment(R.layout.fragment_schedule)
class TeacherClassesFragment : Fragment(R.layout.fragment_dashboard) // Tymczasowy placeholder
class MessagesFragment : Fragment(R.layout.fragment_messages)
class SettingsFragment : Fragment(R.layout.fragment_settings)
