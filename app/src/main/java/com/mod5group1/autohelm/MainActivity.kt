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


        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)


        viewModel.connectBluetooth.observe(this) {
            if (!it) return@observe

            viewModel.setConnectBluetooth(false)

            val permissions = arrayOf(
                BLUETOOTH,
                LOCATION_SERVICE,
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                BLUETOOTH_SERVICE
            )
            requestPermissions(permissions, 0)

            val anyDenied =
                permissions.map { checkSelfPermission(it) }
                    .all { it == PackageManager.PERMISSION_DENIED }

            if (anyDenied) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "No permissions????? :c",
                    Snackbar.LENGTH_LONG
                ).show()
                return@observe
            }
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Doesn't look like bluetooth is working :thinking:",
                    Snackbar.LENGTH_LONG
                ).show()
                return@observe
            }
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (!pairedDevices.any { it.name == "HC-05" }) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Doesn't look like you've paired to the autohelm yet. You should do that first :)",
                    Snackbar.LENGTH_LONG
                ).show()
                return@observe
            }
            bluetoothAdapter.startDiscovery()
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
        private val mutableConnectBluetooth = MutableLiveData<Boolean>()

        val plannedTrajectory: LiveData<Float> get() = mutablePlannedTrajectory
        val currentTrajectory: LiveData<Float> get() = mutableCurrentTrajectory
        val bluetoothSocket: LiveData<BluetoothSocket> get() = mutableBluetoothSocket
        val connectionThread: LiveData<HandleConnectionThread> get() = mutableConnectionThread
        val connectBluetooth: LiveData<Boolean> get() = mutableConnectBluetooth

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

        fun setConnectBluetooth(value: Boolean) {
            mutableConnectBluetooth.value = value
        }
    }


    // Handle found bluetooth devices
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return

                    if (device.name != null) println("Found bluetooth device: ${device.name}")

                    if (device.name == "HC-05") {
                        val bluetoothManager =
                            context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                        bluetoothManager.adapter.cancelDiscovery()

                        val bluetoothSocket =
                            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
                        bluetoothSocket.connect()
                        viewModel.setBluetoothSocket(bluetoothSocket)
                    }
                }
            }
        }
    }
}
