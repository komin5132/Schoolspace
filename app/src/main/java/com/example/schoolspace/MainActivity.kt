package com.example.schoolspace

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // Widoki
        val welcomeText = findViewById<TextView>(R.id.txtWelcome)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Personalizacja powitania
        welcomeText.text = "Witaj, ${user?.email?.split("@")?.get(0) ?: "Użytkowniku"}!"

        // Obsługa wylogowania
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Obsługa menu dolnego
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_schedule -> {
                    Toast.makeText(this, "Plan Lekcji", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_messages -> {
                    Toast.makeText(this, "Wiadomości", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Ustawienia", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}
