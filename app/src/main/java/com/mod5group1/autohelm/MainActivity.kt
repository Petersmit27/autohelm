package com.mod5group1.autohelm

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val viewModel: AutohelmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.currentTrajectory.observe(this, Observer {
            //TODO: handle new trajectory (or push this to the bluetoothfragment
            println("New current trajectory: $it")
        })
        viewModel.plannedTrajectory.observe(this, Observer {
            //TODO: handle new trajectory (or push this to the bluetoothfragment
            println("New planned trajectory: $it")
        })


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            val selectedFragment: Fragment? = when (it.itemId) {
                R.id.nav_bluetooth -> BluetoothFragment()
                R.id.nav_helm -> AutohelmFragment()
                else -> null
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, selectedFragment!!)
                .commit()
            return@setOnItemSelectedListener true
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, AutohelmFragment())
            .commit()

    }

    class AutohelmViewModel : ViewModel() {
        private val mutablePlannedTrajectory = MutableLiveData<Float>()
        private val mutableCurrentTrajectory = MutableLiveData<Float>()

        val plannedTrajectory: LiveData<Float> get() = mutablePlannedTrajectory
        val currentTrajectory: LiveData<Float> get() = mutableCurrentTrajectory

        fun setPlannedTrajectory(trajectory: Float) {
            mutablePlannedTrajectory.value = trajectory
        }

        fun setCurrentTrajectory(trajectory: Float) {
            mutableCurrentTrajectory.value = trajectory
        }
    }

}
