package com.example.schoolspace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Widoki
        val email = findViewById<TextInputEditText>(R.id.etEmail)
        val password = findViewById<TextInputEditText>(R.id.etPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val register = findViewById<TextView>(R.id.txtRegister)
        val googleBtn = findViewById<Button>(R.id.btnGoogleLogin)

        // Logowanie email + hasło
        login.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnSuccessListener {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        goToMain()
                    } else {
                        Toast.makeText(this, "Potwierdź swój e-mail, aby się zalogować.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Złe dane logowania lub brak konta", Toast.LENGTH_LONG).show()
                }
        }

        // Przejście do rejestracji
        register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Konfiguracja Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Klik Google
        googleBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }
    }

    // Obsługa wyniku Google
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Błąd logowania Google", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Firebase Auth z Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                Toast.makeText(this, "Zalogowano przez Google", Toast.LENGTH_SHORT).show()
                goToMain()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd Firebase Auth", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
