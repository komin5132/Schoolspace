package com.example.schoolspace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        btnProfile.setOnClickListener { showProfileDialog() }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_schedule -> ScheduleFragment()
                R.id.nav_messages -> MessagesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            loadFragment(fragment)
            true
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
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogoutDialog)
        val btnAddAccount = dialogView.findViewById<Button>(R.id.btnAddAccount)

        txtEmail.text = user?.email ?: "Użytkownik"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnLogout.setOnClickListener {
            auth.signOut()
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnAddAccount.setOnClickListener {
            Toast.makeText(this, "Dodawanie konta wkrótce", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}

class DashboardFragment : Fragment(R.layout.fragment_dashboard)

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private var calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale("pl", "PL"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val txtDate = view.findViewById<TextView>(R.id.txtDate)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevDay)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextDay)
        val btnRes = view.findViewById<View>(R.id.btnReservations)
        val emptyText = view.findViewById<TextView>(R.id.txtEmptySchedule)
        val listLessons = view.findViewById<View>(R.id.listLessons)

        fun updateUI() {
            txtDate.text = dateFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                emptyText.text = "Wolne"
                emptyText.visibility = View.VISIBLE
                listLessons.visibility = View.GONE
            } else {
                emptyText.text = "Brak planu zajęć"
                emptyText.visibility = View.VISIBLE
                listLessons.visibility = View.GONE
            }
        }

        btnPrev.setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            updateUI()
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            updateUI()
        }

        btnRes.setOnClickListener {
            Toast.makeText(requireContext(), "Rezerwacja Klas - Wkrótce", Toast.LENGTH_SHORT).show()
        }

        updateUI()
    }
}

class MessagesFragment : Fragment(R.layout.fragment_messages)

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val switchDark = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)

        // Odczytujemy AKTUALNY stan motywu, aby wiedzieć jak ustawić switch
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        switchDark?.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES

        // Ustawiamy listenera dopiero PO ustawieniu isChecked, żeby uniknąć pętli
        switchDark?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}
