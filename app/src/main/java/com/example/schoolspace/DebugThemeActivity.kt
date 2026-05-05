package com.example.schoolspace

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class DebugThemeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_theme)

        val switch1 = findViewById<SwitchMaterial>(R.id.debugSwitch1)
        val mockSwitch = findViewById<FrameLayout>(R.id.debugSwitchMock)
        val mockThumb = findViewById<ImageView>(R.id.mockThumb)
        val btnBack = findViewById<Button>(R.id.btnBackFromDebug)

        // Test przełączania wariantu 1
        switch1.setOnCheckedChangeListener { _, isChecked ->
            // Reaguj na zmianę jeśli potrzeba
        }

        // Prosta symulacja dla wariantu 2
        var mockChecked = false
        mockSwitch.setOnClickListener {
            mockChecked = !mockChecked
            mockSwitch.isSelected = mockChecked // trigger selector
            
            val params = mockThumb.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (mockChecked) android.view.Gravity.END else android.view.Gravity.START
            mockThumb.layoutParams = params
            mockThumb.isSelected = mockChecked // trigger selector
        }

        btnBack.setOnClickListener { finish() }
    }
}
