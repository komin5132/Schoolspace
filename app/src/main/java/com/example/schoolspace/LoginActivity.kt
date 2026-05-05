package com.example.schoolspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val register = findViewById<TextView>(R.id.txtRegister)
        val googleBtn = findViewById<Button>(R.id.btnGoogleLogin)

        val prefillEmail = intent.getStringExtra("PREFILL_EMAIL")
        val autoLogin = intent.getBooleanExtra("AUTO_LOGIN", false)

        if (!prefillEmail.isNullOrEmpty()) {
            etEmail.setText(prefillEmail)
            if (autoLogin) {
                val savedPassword = getSavedPassword(prefillEmail)
                if (!savedPassword.isNullOrEmpty()) {
                    etPassword.setText(savedPassword)
                    performLogin(prefillEmail, savedPassword)
                } else {
                    etPassword.requestFocus()
                }
            } else {
                etPassword.requestFocus()
            }
        }

        login.setOnClickListener {
            val emailText = etEmail.text.toString().trim()
            val passwordText = etPassword.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(emailText, passwordText)
        }

        register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }
    }

    private fun performLogin(emailText: String, passwordText: String) {
        auth.signInWithEmailAndPassword(emailText, passwordText)
            .addOnSuccessListener {
                savePassword(emailText, passwordText)
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    goToMain()
                } else {
                    Toast.makeText(this, "Potwierdź swój e-mail, aby się zalogować.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Złe dane logowania lub błąd autoryzacji", Toast.LENGTH_LONG).show()
            }
    }

    private fun getEncryptedPrefs(): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            this,
            "secure_user_creds",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun savePassword(email: String, password: String) {
        getEncryptedPrefs().edit().putString(email, password).apply()
    }

    private fun getSavedPassword(email: String): String? {
        return getEncryptedPrefs().getString(email, null)
    }

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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    val userRef = db.collection("users").document(user.uid)
                    userRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            val userMap = hashMapOf(
                                "uid" to user.uid,
                                "email" to user.email,
                                "role" to "unassigned",
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )
                            userRef.set(userMap).addOnSuccessListener {
                                goToMain()
                            }
                        } else {
                            goToMain()
                        }
                    }
                }
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
