package com.mod5group1.autohelm

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import java.lang.System.*
import java.util.*

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


        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)
        val connectButton = view.findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {
            //Array of necessary permissions
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
//                AppCompatActivity.LOCATION_SERVICE,
                Manifest.permission.ACCESS_FINE_LOCATION,
//                AppCompatActivity.BLUETOOTH_SERVICE,
                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.BLUETOOTH_SCAN,
            )
            //Request the permissions
            requireActivity().requestPermissions(permissions, 0)
            val hasSystemFeature =
                requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            for (p in permissions) {
                val permission = requireActivity().checkSelfPermission(p)
                if (permission == PERMISSION_DENIED) {
                    Snackbar.make(
                        view.findViewById(android.R.id.content),
                        "No permissions????? :c",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }
//            val anyDenied =
//                permissions.map { requireActivity().checkSelfPermission(it) }
//                    .any { it == PackageManager.PERMISSION_DENIED }
//
//            if (anyDenied) {
//                Snackbar.make(
//                    view.findViewById(android.R.id.content),
//                    "No permissions????? :c",
//                    Snackbar.LENGTH_LONG
//                ).show()
//                return@setOnClickListener
//            }
            val bluetoothManager =
                requireContext().getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Snackbar.make(
                    view.findViewById(android.R.id.content),
                    "Doesn't look like bluetooth is working :thinking:",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (!pairedDevices.any { it.name == "HC-05" }) {
                Snackbar.make(
                    view.findViewById(android.R.id.content),
                    "Doesn't look like you've paired to the autohelm yet. You should do that first :)",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }


            bluetoothAdapter.startDiscovery()
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


                        val bluetoothSocket =
                            device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        bluetoothSocket.connect()

                        viewModel.setBluetoothSocket(bluetoothSocket)
                        bluetoothManager.adapter.cancelDiscovery()
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
