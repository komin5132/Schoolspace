package com.example.schoolspace

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Domyślnie sprawdź czy system jest w trybie ciemnym jeśli nie ustawiono inaczej
        val isDarkMode = prefs.getBoolean("dark_mode",
            (requireContext().resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        )
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Recreate activity to apply theme, MainActivity will restore fragment via onSaveInstanceState
            requireActivity().recreate()
        }
    }
}
