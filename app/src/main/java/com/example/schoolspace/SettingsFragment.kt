package com.example.schoolspace

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = view.findViewById<FrameLayout>(R.id.switchDarkModeContainer)
        val thumbBg = view.findViewById<View>(R.id.switchThumbBackground)
        val thumbIcon = view.findViewById<ImageView>(R.id.switchDarkModeThumbIcon)
        val bgSun = view.findViewById<ImageView>(R.id.bgSun)
        val bgMoon = view.findViewById<ImageView>(R.id.bgMoon)
        
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        var isDarkMode = prefs.getBoolean("dark_mode", 
            (requireContext().resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        )

        updateSwitchUI(container, thumbBg, thumbIcon, bgSun, bgMoon, isDarkMode)
        
        container.setOnClickListener {
            isDarkMode = !isDarkMode
            prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
            
            updateSwitchUI(container, thumbBg, thumbIcon, bgSun, bgMoon, isDarkMode)

            if (isDarkMode) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
            
            requireActivity().recreate()
        }
    }

    private fun updateSwitchUI(container: FrameLayout, thumbBg: View, thumbIcon: ImageView, bgSun: ImageView, bgMoon: ImageView, isDarkMode: Boolean) {
        container.isSelected = isDarkMode
        
        // Aktywna ikona na fioletowym kółku (zawsze biała)
        if (isDarkMode) {
            thumbIcon.setImageResource(R.drawable.ic_moon)
        } else {
            thumbIcon.setImageResource(R.drawable.ic_sun)
        }
        thumbIcon.setColorFilter(android.graphics.Color.WHITE)

        // Ikony w tle (szare)
        val greyColor = if (isDarkMode) android.graphics.Color.parseColor("#444444") else android.graphics.Color.parseColor("#CCCCCC")
        bgSun.setColorFilter(greyColor)
        bgMoon.setColorFilter(greyColor)

        // Przesuń tło kółka
        val bgParams = thumbBg.layoutParams as FrameLayout.LayoutParams
        bgParams.gravity = if (isDarkMode) android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL else android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        thumbBg.layoutParams = bgParams

        // Przesuń ikonę na kółku
        val iconParams = thumbIcon.layoutParams as FrameLayout.LayoutParams
        iconParams.gravity = if (isDarkMode) android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL else android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        thumbIcon.layoutParams = iconParams
    }
}
