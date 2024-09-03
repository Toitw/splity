package com.example.splity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_groups -> switchFragment(GroupsFragment())
                R.id.nav_expenses -> switchFragment(ExpensesFragment())
                R.id.nav_balance -> switchFragment(BalanceFragment())
            }
            true
        }

        // Set the initial fragment if savedInstanceState is null
        if (savedInstanceState == null) {
            switchFragment(GroupsFragment())
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}