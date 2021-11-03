package com.mod5group1.autohelm

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * A simple [Fragment] subclass.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BluetoothFragment : Fragment() {

    private var bluetoothSocket: BluetoothSocket? = null

    private val viewModel: MainActivity.AutohelmViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val inflateView = inflater.inflate(R.layout.fragment_bluetooth, container, false)

        val bluetoothOutput = inflateView.findViewById<TextView>(R.id.bluetoothOutput)
        for (i in 0..100) {
            bluetoothOutput.text = bluetoothOutput.text.toString() + "bruhhhhhh\n"
            println("bruhhhhhh")
        }

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)

        val connectButton = inflateView.findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {

            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH), 0)

            val bluetoothPermission =
                this.requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH)
            println("BLUETOOTH PERMISSION IS: $bluetoothPermission")

            if (bluetoothPermission == PackageManager.PERMISSION_DENIED) {
                Snackbar.make(
                    connectButton,
                    "No bluetooth permissions????? :c",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }


            val bluetoothManager =
                this.requireContext()
                    .getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Snackbar.make(
                    connectButton,
                    "Doesn't look like bluetooth is working :thinking:",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
//            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
//            if (!pairedDevices.any { it.name == "HC-05" }) {
//                Snackbar.make(
//                    connectButton,
//                    "Doesn't look like you've paired to the autohelm yet. You should do that first :)",
//                    Snackbar.LENGTH_LONG
//                ).show()
//                return@setOnClickListener
//            }
            bluetoothAdapter.startDiscovery()
        }



        return inflateView
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
                            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
                        bluetoothManager.adapter.cancelDiscovery()

                        val bluetoothSocket =
                            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
                        bluetoothSocket.connect()
                        this@BluetoothFragment.bluetoothSocket = bluetoothSocket
                        HandleConnectionThread(bluetoothSocket).start()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(receiver)
    }

    private inner class HandleConnectionThread(val socket: BluetoothSocket) : Thread() {

        val inputReader = BufferedReader(InputStreamReader(socket.inputStream))
        val outputReader = BufferedWriter(OutputStreamWriter(socket.outputStream))


        override fun run() {
            socket.use {
                while (true) {
                    while (inputReader.ready()) {
                        val message = inputReader.readLine()
                        println("Recieved message from bluetooth connection: $message")

                        if (message.startsWith("trajectory ")) {
                            val trajectory = message.split("trajectory ")[1].toFloat()
                            viewModel.setCurrentTrajectory(trajectory)
                        }
                    }
                }
            }
        }
    }
}
