package com.example.schoolspace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class PhoneLoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etSmsCode: TextInputEditText
    private lateinit var btnSendSms: Button
    private lateinit var btnVerifyCode: Button
    private lateinit var btnBack: ImageButton
    private lateinit var txtBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_login)
        
        auth = FirebaseAuth.getInstance()

        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etSmsCode = findViewById(R.id.etSmsCode)
        btnSendSms = findViewById(R.id.btnSendSms)
        btnVerifyCode = findViewById(R.id.btnVerifyCode)
        btnBack = findViewById(R.id.btnBack)
        txtBackToLogin = findViewById(R.id.txtBackToLogin)

        // Powrót do logowania
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        txtBackToLogin.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 1. Wysyłanie SMS
        btnSendSms.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString() // np. +48123456789
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Wprowadź numer telefonu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        // 2. Weryfikacja kodu z SMS
        btnVerifyCode.setOnClickListener {
            val code = etSmsCode.text.toString()
            if (code.isEmpty()) {
                Toast.makeText(this, "Wprowadź kod SMS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Najpierw wyślij kod SMS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            Toast.makeText(this@PhoneLoginActivity, "Kod wysłany!", Toast.LENGTH_SHORT).show()
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@PhoneLoginActivity, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnSuccessListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Błąd logowania: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
