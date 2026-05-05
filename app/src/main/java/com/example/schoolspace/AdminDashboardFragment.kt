package com.example.schoolspace

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.cardManageUsers)?.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ManageUsersFragment(), true)
        }
        view.findViewById<View>(R.id.cardManageSchedule)?.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ManageScheduleFragment(), true)
        }
        view.findViewById<View>(R.id.cardManageGrades)?.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ManageGradesFragment(), true)
        }
    }
}
