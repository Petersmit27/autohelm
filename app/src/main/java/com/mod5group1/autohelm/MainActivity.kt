package com.mod5group1.autohelm

import android.Manifest.permission.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private val viewModel: AutohelmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.currentTrajectory.observe(this) {
            //TODO: handle new trajectory (or push this to the bluetoothfragment
            println("New current trajectory: $it")
        }
        viewModel.bluetoothSocket.observe(this) {
            val thread = HandleConnectionThread(it)
            thread.start()
            viewModel.setConnectionThread(thread)
        }
        viewModel.plannedTrajectory.observe(this) {
            println("New planned trajectory: $it")
            val connectionThread = viewModel.connectionThread.value
            if (connectionThread != null) {
                connectionThread.sendMessage("trajectory $it")
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "You're not connected to the autohelm yet, maybe do that first",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }


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


    inner class HandleConnectionThread(private val socket: BluetoothSocket) : Thread() {

        private val inputReader = BufferedReader(InputStreamReader(socket.inputStream))
        private val outputReader = BufferedWriter(OutputStreamWriter(socket.outputStream))

        override fun run() {
            socket.use {
                while (true) {
                    while (inputReader.ready()) {
                        val message = inputReader.readLine()
                        println("Recieved message from bluetooth connection: $message")

                        if (message.startsWith("trajectory ")) {
                            val trajectory = message.split("trajectory ")[0].toFloat()
                            viewModel.setCurrentTrajectory(trajectory)
                        }
                    }
                }
            }
        }

        fun sendMessage(message: String) {
            outputReader.write(message)
            outputReader.newLine()
            outputReader.flush()
        }
    }

    class AutohelmViewModel : ViewModel() {
        private val mutablePlannedTrajectory = MutableLiveData<Float>()
        private val mutableCurrentTrajectory = MutableLiveData<Float>()
        private val mutableBluetoothSocket = MutableLiveData<BluetoothSocket>()
        private val mutableConnectionThread = MutableLiveData<HandleConnectionThread>()

        val plannedTrajectory: LiveData<Float> get() = mutablePlannedTrajectory
        val currentTrajectory: LiveData<Float> get() = mutableCurrentTrajectory
        val bluetoothSocket: LiveData<BluetoothSocket> get() = mutableBluetoothSocket
        val connectionThread: LiveData<HandleConnectionThread> get() = mutableConnectionThread

        fun setPlannedTrajectory(trajectory: Float) {
            mutablePlannedTrajectory.value = trajectory
        }

        fun setCurrentTrajectory(trajectory: Float) {
            mutableCurrentTrajectory.value = trajectory
        }

        fun setBluetoothSocket(socket: BluetoothSocket) {
            mutableBluetoothSocket.value = socket
        }

        fun setConnectionThread(thread: HandleConnectionThread) {
            mutableConnectionThread.value = thread
        }
    }

}
