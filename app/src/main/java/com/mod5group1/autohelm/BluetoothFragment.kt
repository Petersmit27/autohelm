package com.mod5group1.autohelm

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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

/**
 * A simple [Fragment] subclass.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BluetoothFragment : Fragment() {

    private val viewModel: MainActivity.AutohelmViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bluetooth, container, false)

        val bluetoothOutput = view.findViewById<TextView>(R.id.bluetoothOutput)


        val connectButton = view.findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {
            viewModel.setConnectBluetooth(true)
        }

        viewModel.currentTrajectory.observe(requireActivity()) {
            bluetoothOutput.text = bluetoothOutput.text.toString() + "Current trajectory: $it"
        }

        return view
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
                        viewModel.setBluetoothSocket(bluetoothSocket)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(receiver)
    }

}
