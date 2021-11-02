package com.mod5group1.autohelm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            val selectedFragment: Fragment? = when (it.itemId) {
                R.id.nav_bluetooth -> BluetoothFragment()
                R.id.nav_helm -> AutohelmFragment()
                else -> null
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer,selectedFragment!!)
                .commit()
            return@setOnItemSelectedListener true
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer,AutohelmFragment())
            .commit()

    }

}
